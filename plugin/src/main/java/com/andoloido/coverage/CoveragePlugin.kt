package com.andoloido.coverage

import com.andoloido.coverage.utils.MappingIdGen
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoveragePlugin : Plugin<Project> {

    companion object {
        const val REPORTER_CLASS = "com/zhihu/android/community_base/CoverageReporter"
        const val REPORTER_METHOD = "Report"
        const val EXTENSION_NAME = "coverageExt"
    }

    lateinit var mappingIdGen: MappingIdGen

    override fun apply(project: Project) {
        val basePath = File(File(project.buildDir, "Coverage"), "cov").path
        val mappingFilePath = "$basePath${File.separator}mapping_${
            SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA).format(
                Date()
            )}.txt"
        val latestFilePath = "$basePath${File.separator}mapping_latest.txt"

        mappingIdGen = MappingIdGen(
            latestFilePath,
            mappingFilePath
        )
        project.extensions.create(EXTENSION_NAME, CoverageExtension::class.java)
        val transform = CoverageTransform(mappingIdGen)
        project.extensions.getByType(AppExtension::class.java).registerTransform(transform)
        project.afterEvaluate {
            val ext =  project.extensions.getByType(CoverageExtension::class.java)
            transform.ext = ext
        }
    }
}