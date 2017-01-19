FROM java:jdk
ENV DEBIAN_FRONTEND noninteractive
# Dependencies
RUN dpkg --add-architecture i386 && apt-get update && apt-get install -yq libstdc++6:i386 zlib1g:i386 libncurses5:i386 lib32z1 ant maven --no-install-recommends
ENV GRADLE_URL http://services.gradle.org/distributions/gradle-2.2.1-all.zip
RUN curl -L ${GRADLE_URL} -o /tmp/gradle-2.2.1-all.zip && unzip /tmp/gradle-2.2.1-all.zip -d /usr/local && rm /tmp/gradle-2.2.1-all.zip
ENV GRADLE_HOME /usr/local/gradle-2.2.1
# Download and untar SDK
ENV ANDROID_SDK_URL http://dl.google.com/android/android-sdk_r24.4.1-linux.tgz
RUN curl -L ${ANDROID_SDK_URL} | tar xz -C /usr/local
#ENV ANDROID_SDK_HOME /usr/local/android-sdk-linux
ENV ANDROID_HOME /usr/local/android-sdk-linux
# Install Android SDK components
ENV ANDROID_SDK_DATE 20160203
ENV ANDROID_SDK_COMPONENTS platform-tools,build-tools-23.0.3,build-tools-23.0.2,build-tools-23.0.1,build-tools-22.0.1,build-tools-21.1.2,build-tools-21.1.1,android-23,android-22,android-21,extra-android-m2repository,extra-google-m2repository
RUN ( sleep 5 && while [ 1 ]; do sleep 1; echo y; done ) | ${ANDROID_HOME}/tools/android update sdk --no-ui --all --filter "${ANDROID_SDK_COMPONENTS}"
# Install Android NDK
ENV NDK_URL http://dl.google.com/android/ndk/android-ndk-r10d-linux-x86_64.bin
RUN curl ${NDK_URL} -o /tmp/android-ndk-r10d-linux-x86_64.bin && chmod +x /tmp/android-ndk-r10d-linux-x86_64.bin && \
    cd /usr/local && /tmp/android-ndk-r10d-linux-x86_64.bin && rm /tmp/android-ndk-r10d-linux-x86_64.bin
ENV NDK_HOME /usr/local/android-ndk-r10d
# Path
ENV PATH $PATH:${ANDROID_HOME}/tools:${ANDROID_HOME}/platform-tools:${GRADLE_HOME}/bin:${NDK_HOME}
