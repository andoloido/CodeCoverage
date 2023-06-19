package com.andoloido.coverage

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoveragePlugin : Plugin<Project> {

    lateinit var mappingIdGen: MappingIdGen

    override fun apply(project: Project) {
        println("register transform")
        val basePath = File(File(project.buildDir, "Coverage"), "cov").path
        val mappingFilePath = "$basePath${File.separator}mapping_${
            SimpleDateFormat("yyyyMMddHHmm", Locale.CHINA).format(
                Date()
            )}.txt"

        val latestFilePath = "$basePath${File.separator}mapping_latest.txt"
        mappingIdGen = MappingIdGen(latestFilePath, mappingFilePath)
        project.extensions.getByType(AppExtension::class.java).registerTransform(CustomTransform(mappingIdGen))
    }
}