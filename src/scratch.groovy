#!/usr/bin/env groovy

@Grab(group = 'net.bramp.ffmpeg', module = 'ffmpeg', version = '0.6.2')
@Grab(group='com.github.mjeanroy', module = 'exiftool-lib', version = '2.1.0')

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.job.FFmpegJob
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.Progress
import net.bramp.ffmpeg.progress.ProgressListener

//------------------------------------------------------------------------
// STEP 0: CLI arguments

if (args.size() != 2) {
    println('\nUsage: scratch /path/to/sourcefolder /path/to/outputFolder\n')
    System.exit(1)
}

def sourceFolder = new File(args[0])
def outputFolder = new File(args[1])

def ffmpegBin = "which ffmpeg".execute().text.replace("\n", "")
def ffprobeBin = "which ffprobe".execute().text.replace("\n", "")

ffmpeg = new FFmpeg(ffmpegBin)
ffprobe = new FFprobe(ffprobeBin)

//------------------------------------------------------------------------
// STEP 1: Scan the folder structure

photoFiles = []
videoFiles = []
safeToDeleteFiles = []
ignoredFiles = []
numberOfFoldersExplored = 1

dive(sourceFolder)

//------------------------------------------------------------------------
// STEP 2: Organize the files


//------------------------------------------------------------------------
// STEP 3: Clean and Exit








def dive(File folder) {
    println("DIVING IN: ${folder.name}")
    File[] files = folder.listFiles()
    for (File file : files) {
        def extension = getFileExtension(file)

        if (Extensions.video.contains(extension)) {
            videoFiles.add(file)
        } else if (Extensions.photo.contains(extension)) {
            photoFiles.add(file)
        } else if (Extensions.meta.contains(extension)) {
            safeToDeleteFiles.add(file)
        } else if (file.isDirectory() && file.listFiles().size() == 0){ //TODO: Effective?
            safeToDeleteFile.add(file)
        } else if (file.isDirectory()) {
            numberOfFoldersExplored++
            dive(file)
        } else {
            ignoredFiles.add(file)
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
    } else if (lastIndexOf == -1 & file.isFile()) {
        return "NOEXT"
    } else{
        // It is a folder
    }

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
// Transcoding video files

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
// Converting photo files