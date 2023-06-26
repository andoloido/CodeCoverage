package com.andoloido.coverage

import com.andoloido.coverage.model.WhiteList
import com.andoloido.coverage.utils.MappingIdGen
import com.android.build.api.transform.*
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class CoverageTransform(val idGen: MappingIdGen) : Transform() {

    lateinit var ext: CoverageExtension

    val whiteList by lazy {
        WhiteList.buildWithList(ext.whiteList)
    }

    override fun getName(): String = "CoverageTransform"
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> = mutableSetOf(
        QualifiedContent.Scope.PROJECT,
        QualifiedContent.Scope.SUB_PROJECTS,
        QualifiedContent.Scope.EXTERNAL_LIBRARIES
    )

    override fun isIncremental(): Boolean = false

    override fun transform(transformInvocation: TransformInvocation) {
        println("--------------- coverage transform visit start --------------- ")
        val startTime = System.currentTimeMillis()

        val outputProvider = transformInvocation.outputProvider
        outputProvider?.deleteAll()

        transformInvocation.inputs.forEach { transformInput ->
            transformInput.directoryInputs.forEach { directoryInput ->
                handleDirectory(directoryInput, outputProvider)
            }
            transformInput.jarInputs.forEach { jarInput ->
                handleJar(jarInput, outputProvider)
            }
        }

        val cost = (System.currentTimeMillis() - startTime) / 1000
        println("--------------- coverage transform visit end --------------- ")
        println("coverage transform cost: $cost s")
    }

    private fun handleDirectory(
        directoryInput: DirectoryInput, outputProvider: TransformOutputProvider
    ) {
        if (directoryInput.file.isDirectory) {
            directoryInput.file.walk().filter { it.isFile && it.extension == "class" }
                .forEach { classFile ->
                    if (checkClassFile(classFile.relativeTo(directoryInput.file).path)) {
                        println("----------- deal with \"class\" file <${classFile.name}> -----------")
                        val classReader = ClassReader(classFile.readBytes())
                        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                        val classVisitor = CoverageClassVisitor(classWriter, idGen)
                        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                        val codeBytes = classWriter.toByteArray()
                        FileOutputStream(classFile).use { it.write(codeBytes) }
                    }
                }
        }
        val destFile = outputProvider.getContentLocation(
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY
        )
        FileUtils.copyDirectory(directoryInput.file, destFile)
    }

    private fun handleJar(jarInput: JarInput, outputProvider: TransformOutputProvider) {
        if (jarInput.file.absolutePath.endsWith(".jar")) {
            val jarName = jarInput.name.removeSuffix(".jar")
            val md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()
            val tmpFile = File(jarInput.file.parentFile, "classes_temp.jar")
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            val jarOutputStream = JarOutputStream(FileOutputStream(tmpFile))

            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(jarEntry)
                if (entryName.endsWith(".class") && checkClassFile(entryName)) {
                    println("----------- deal with \"jar\" class file <$entryName> -----------")
                    jarOutputStream.putNextEntry(zipEntry)
                    val classReader = ClassReader(inputStream.readBytes())
                    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    val classVisitor = CoverageClassVisitor(classWriter, idGen)
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
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

            val dest = outputProvider.getContentLocation(
                jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR
            )
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }

    private fun checkClassFile(name: String): Boolean {
        return whiteList.inWhiteList(name)
//        return (!name.startsWith("R\$") && "R.class" != name && "BuildConfig.class" != name && name.startsWith("com/example/myapplication"))
    }
}