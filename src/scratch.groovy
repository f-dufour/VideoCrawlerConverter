#!/usr/bin/env groovy

@Grab(group = 'net.bramp.ffmpeg', module = 'ffmpeg', version = '0.6.2')

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.job.FFmpegJob
import net.bramp.ffmpeg.progress.ProgressListener
import net.bramp.ffmpeg.progress.Progress
import java.util.concurrent.TimeUnit

//------------------------------------------------------------------------
// CLI arguments

if (args.size() != 2) {
    println('\nUsage: scratch /path/to/sourcefolder /path/to/outputFolder\n')
    System.exit(1)
}

def sourceFolder = new File(args[0])
def outputFolder = new File(args[1])

ffmpeg = new FFmpeg("/usr/local/bin/ffmpeg")
ffprobe = new FFprobe("/usr/local/bin/ffprobe")

//------------------------------------------------------------------------
// Core

def referenceVideoExtension = [".MTS", ".mts", ".m2ts"]
def referenceMetaExtension = [".mta", ".modd", ".DS_Store"]

filesToTranscode = []
filesToDelete = []
numberOfFoldersExplored = 1

def dive(File folder, ArrayList<String> extensionsToTranscode, ArrayList<String> extensionsToDelete) {
    println("DIVING IN: ${folder.name}")
    File[] files = folder.listFiles()

    for (File file : files) {
        def extension = getFileExtension(file)
        // File has to be transcoded
        if (extensionsToTranscode.contains(extension)) {
            filesToTranscode.add(file)
            // File has to be deleted
        } else if (extensionsToDelete.contains(extension)) {
            filesToDelete.add(file)
            // It is a folder
        } else if (file.isDirectory()) {
            dive(file, extensionsToTranscode, extensionsToDelete)
            numberOfFoldersExplored++
        } else {
            println("\tFile ${file.name} with extension ${extension} is ignored")
        }
    }
}

//------------------------------------------------------------------------
// Helper methods

def getFileExtension(File file) {
    String name = file.getName()
    int lastIndexOf = name.lastIndexOf(".")
    if (lastIndexOf != -1) {
        return name.substring(lastIndexOf)
    }
    return "NO EXTENSION FOR FILE ${file.name}"
}

def getFileNameWithoutExtension(File file) {
    String name = file.getName()
    if (name.indexOf(".") > 0) {
        return name.substring(0, name.lastIndexOf("."))
    } else {
        return name
    }
}

//------------------------------------------------------------------------
// Crawling

println("\nRUN:")
println("===\n")

dive(sourceFolder, referenceVideoExtension, referenceMetaExtension)
println("\n> ${numberOfFoldersExplored} folders explored")

println("\nSUMMARY:")
println("=======")
println("\n(${filesToTranscode.size()}) Files to willTranscode:")
filesToTranscode.forEach({
    println(it.getAbsolutePath())
})

println("\n(${filesToDelete.size()}) Files to delete:")
filesToDelete.forEach({
    println(it.getAbsolutePath())
})

//------------------------------------------------------------------------
// FFmpeg stuff

def ffmpegTranscode(File inputFile, File outputFolder) {

    def inputFilename = getFileNameWithoutExtension(inputFile)
    def outputFilePath = new StringBuilder()
            .append(outputFolder.absolutePath)
            .append("/")
            .append(inputFilename)
            .append(".mov")
            .toString()

    FFmpegProbeResult probeResult = ffprobe.probe(inputFile.absolutePath)
    FFmpegBuilder builder = new FFmpegBuilder()

            .setInput(probeResult)
            .overrideOutputFiles(true)

            .addOutput(outputFilePath)
            .setFormat("mov")
            .done()

    // Prepare execution

    FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe)
    FFmpegJob job = executor.createJob(builder, new ProgressListener() {

        // Using the FFmpegProbeResult determine the duration of the input
        double duration_ns = probeResult.getFormat().duration * TimeUnit.SECONDS.toNanos(1)

        @Override
        void progress(Progress progress) {
            double percentage = progress.out_time_ns / duration_ns

            // Print out interesting information about the progress
            //println("${outputFilePath}:")
            print(String.format("\rTranscoding ${inputFile.name} (%.0f%%)", percentage * 100))
        }
    })

    // RUN!

    job.run()
}

//------------------------------------------------------------------------
// User input

print("\nWould you like to trascode the ${filesToTranscode.size()} files? (y/n): ")
def willTranscode = System.in.newReader().readLine()
print("Would you like to delete the ${filesToDelete.size()} files? (y/n): ")
def willDelete = System.in.newReader().readLine()
println("")

//------------------------------------------------------------------------
// Process

//TODO: time

if (willTranscode == "y") {
    filesToTranscode.forEach({
        ffmpegTranscode(it, outputFolder)
    })
} else {
    println("${filesToTranscode.size()} files are not transcoded")
}

if (willDelete == "y") {
    filesToDelete.forEach({
        println("Deleting ${it.absolutePath}")
        it.delete()
    })
} else {
    println("\n${filesToDelete.size()} files stay in place and are not deleted")
}

//------------------------------------------------------------------------
// Clean up

print("\nWould you like to delete original videos? (${filesToTranscode.size()} files) (y/n): ")
def deleteOriginals = System.in.newReader().readLine()

if (deleteOriginals == "y") {
    filesToTranscode.forEach({
        println("Deleting ${it.absolutePath}")
        it.delete()
    })
} else {
    println("${filesToTranscode.size()} files are not deleted")
}

//TODO write log


System.exit(0)