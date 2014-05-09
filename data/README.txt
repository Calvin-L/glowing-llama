Data files associated with the project. These are not compiled into the final
Android app.

    audio.txt.gz           Gzipp'd audio samples taken from a demo run. It
                           contains 3 0xAAAAAAAA preambles using amplitude
                           modulation.
                           Target frequency: 4410 Hz
                           Sample rate: 44100 Hz
                           Samples/bit: 80

    audio-XX.txt.gz        Gzipp'd audio samples taken from demo runs. Contains
    raw_audio-XX.txt.gz    varying amounts of information. The audio-XX files
                           contain max-filtered audio while the raw_audio-XX
                           files contain the raw audio samples. Frequency and
                           other stats are the same as audio.txt.gz.

    accuracy_data.xlsx     Bit transfer accuracy data taken at various phone
                           distances. Has a few false starts; the real data is
                           in the last table.
