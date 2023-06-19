package com.andoloido.coverage

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class CustomTransform(val idGen: MappingIdGen) : Transform() {

    override fun getName() = "CustomTransform"


    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        //需要处理的数据类型,这里表示class文件
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        //作用范围
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation) {
        println("start transform")
        super.transform(transformInvocation)

        //TransformOutputProvider管理输出路径,如果消费型输入为空,则outputProvider也为空
        val outputProvider = transformInvocation.outputProvider

        //当前是否是增量编译,由isIncremental方法决定的
        // 当上面的isIncremental()写的返回true,这里得到的值不一定是true,还得看当时环境.比如clean之后第一次运行肯定就不是增量编译嘛.
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            //不是增量编译则删除之前的所有文件
            outputProvider.deleteAll()
        }

        transformInvocation.inputs?.forEach { transformInput ->
            println(transformInput)
            transformInput.directoryInputs.forEach { directoryInput ->
                processDirectoryInput(transformInvocation, directoryInput)
            }
            transformInput.jarInputs.forEach { jarInput ->
                handleJar(transformInvocation, jarInput)
            }
        }
        afterTransform()
    }

    private fun afterTransform() {
        idGen.saveMapping()
        idGen.clean()
    }

    private fun processJarInput(
        outputProvider: TransformInvocation,
        jarInput: JarInput
    ) {
        val dest = outputProvider.outputProvider.getContentLocation(
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
        println(dest)

        jarInput.file.copyTo(dest, true)
    }

    fun handleJar(transformInvocation: TransformInvocation, jarInput: JarInput) {
        if (jarInput.file.absolutePath.endsWith(".jar")) {
            // 截取文件路径的 md5 值重命名输出文件，避免出现同名而覆盖的情况出现
//            var jarName = jarInput.name
//            val md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
//            if (jarName.endsWith(".jar")) {
//                jarName = jarName.substring(0, jarName.length - 4)
//            }
            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()
            val tmpFile = File(jarInput.file.parent + File.separator + "classes_temp.jar")
            // 避免上次的缓存被重复插入
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(jarEntry)
                if (isNeedTraceClass(entryName)) {
                    // 使用 ASM 对 class 文件进行操控
                    println("----------- deal with \"jar\" class file <$entryName> -----------")
                    jarOutputStream.putNextEntry(zipEntry)
                    val classReader = ClassReader(inputStream.readBytes())
                    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    val cv = CoverageClassVisitor(classWriter, idGen)
                    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
                    val code = classWriter.toByteArray()
                    jarOutputStream.write(code)
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(inputStream.readBytes())
                }
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            jarFile.close()

            val dest = transformInvocation.outputProvider.getContentLocation(
                jarInput.name,
                jarInput.contentTypes,
                jarInput.scopes,
                Format.JAR
            )

            // 生成输出路径 dest：./app/build/intermediates/transforms/xxxTransform/...
//            val dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
//                jarInput.contentTypes, jarInput.scopes, Format.JAR)
            // 将 input 的目录复制到 output 指定目录
            tmpFile.copyTo(dest, true)
            tmpFile.delete()
        }
    }

    private fun processDirectoryInput(
        transformInvocation: TransformInvocation,
        directoryInput: DirectoryInput
    ) {
        val dest = transformInvocation.outputProvider.getContentLocation(
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY
        )
        println(dest)
        directoryInput.file.walk().forEach { file ->
            val destFile = File(dest, file.path.substring(directoryInput.file.path.length))
            if (file.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile.mkdirs()
                copyFile(file, destFile)
            }
        }
    }

    private fun copyFile(inputFile: File, outputFile: File) {
        if (!isNeedTraceClass(inputFile.name)) return
        val inputStream = FileInputStream(inputFile)
        val outputStream = FileOutputStream(outputFile)

        //1. 构建ClassReader对象
        val classReader = ClassReader(inputStream)
        //2. 构建ClassVisitor的实现类ClassWriter
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        //3. 将ClassReader读取到的内容回调给ClassVisitor接口
        classReader.accept(CoverageClassVisitor(classWriter, idGen), ClassReader.EXPAND_FRAMES)
        //4. 通过classWriter对象的toByteArray方法拿到完整的字节流
        outputStream.write(classWriter.toByteArray())

        inputStream.close()
        outputStream.close()
    }

    fun isNeedTraceClass(name: String): Boolean {
        if (!name.endsWith(".class")
            || name.startsWith("R.class")
            || name.startsWith("R$")
        ) {
            return false
        }
        return true
    }
}