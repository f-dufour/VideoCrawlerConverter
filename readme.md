# Introduction

This is a one life groovy script. It has been created to prepare a folder of unorganized photos/videos to be imported in photos.app

Usage:
```shell
~$ ./scratch.groovy /path/input/folder /path/output/folder
```

Dependencies:
- **FFmpeg wrapper**: To transcode the video files not compatible with photos.app 

# Improvements

- Use hashmaps to keep count for each file type. 
- Read meta of each file and sort files by year/month.
- Filter images (small pngs are not proper pictures). 

# Make sure

- Photos.app is able to detect double imports.
- Use folder names as keywords.
- Can ffmpeg deal with m2ts et 3gp.