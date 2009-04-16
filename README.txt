Introduction
============

This chat application is an example of how you can port the java cometd client to android.
This technique allows any android app to talk to a server through firewalls (like most browser applications do).
The org.mortbay.cometd.client.BayeuxClient takes care of all the dirty work for you. (bayeux protocol)


The following core dependencies were used:
+  jetty-client-6.1.12.rc2 (asynchronous http client)
+  bayeux-client-6.1.12.rc2 (bayeux protocol via http)
+  android-1.0_r1

(See pom.xml)


How to build
============

x. Make sure ANDROID_HOME is set (path to your android sdk installation)

x. Install the android.jar from your android install directory into your local maven repository:
   $ mvn install:install-file -DgroupId=android -DartifactId=android \
        -Dversion=[android version] -Dpackaging=jar \
        -Dfile=[path to android.jar]

x. Excecute:
   $ mvn -Dandroid.home=$ANDROID_HOME clean package
 
   for windows thats %ANDROID_HOME%
   


Installing
============

x. Ensure that you have the Android tools directory in your path.


x. Start the Android emulator if you didn't do it in the previous step:
   $ emulator

   You will need to wait a few moments for the emulator to start. 
   When you see the android icon start to pulsate on the screen
   you are ready to proceed to the next step.


x. Copy the application bundle to the Android emulator using the adb utility:

  $ adb install target/cometdchat.apk


x. To test locally, run the cometd chat demo.
  $ cd $jetty6.home/contrib/cometd/demos/
  $ mvn jetty:run
  
   To chat with other users on the internet, change your connection settings:
   host: cometdchat.morphexchange.com
   port: 80