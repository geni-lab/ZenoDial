ZenoDial
========
A Java-based dialogue system with JMegaHAL and Wolfram|Alpha integrated.


Prerequisites
-------------
Have the following installed / running on the computer that is going to compile and run ZenoDial:
- JDK 7 or above
- ant
- rosjava indigo
- itf_listen.py
- itf_talk.py

Sign up a Wolfram|Alpha API key (https://developer.wolframalpha.com/portal/apisignup.html) and paste it to the variable "API_ID" in "chatbot.plugin.WolframAlpha".


How to run it
-------------
Clone the project into your workspace, then execute the commands in sequence:

ant

ant ChatBot


Note
----
You can communicate with ZenoDial via command line but currently that interface is a mess, so I don't recommand you to do so...
