# TinyMFT
TinyMFT is a multi-protocol file transfer gateway. It supports the following protocols:
* HTTPS - [Powered by Jetty](https://www.eclipse.org/jetty)
* FTPS - [Powered by Apache Mina](https://mina.apache.org/ftpserver-project/index.html)
* SFTP - [Powered by Apache Mina](https://mina.apache.org/sshd-project/index.html)

TinyMFT supports dual authentication using password and certificate/key cobmination. The authentication is pluggable and utlizes [JAAS](https://en.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service) so that any kind of user store can be utlized. The sample plugin reads credentials from a flat file and authenticates the user either using password or certificate/password or both.

Additionally, [Apache Camel](http://camel.apache.org/) provides the integration engine which lets you take care of complicated routing and transformation of files.

## Configuration
The config.properties file in the conf folder contains the configuration parameters used to run TinyMFT.

* JAASCONFIG - Specifies the location of JAAS config file
* JAASDOMAIN - JAAS domain to be used for authentication
* BINDIP - Interface IP to bind the ports
* SSHPORT - SSH Port
* FTPPORT - FTP Port
* HTTPPORT - HTTP Port
* KEYSTORE - Keystore location containing the server certificate
* KEYSTOREPASSWORD - Keystore password
* KEYPASSWORD - Private Key password
* KEYALIAS - Alias of certificate in the keystore
* TRUSTSTORE - Location of the trust store
* TRUSTSTOREPASSWORD - Trust store password
* ROOTFOLDER - Folder under which home folder of each account will be created
* ROUTESFOLDER - Location of all camel routes
* DUALAUTH - Enable dual authentication (true/false)
* SECRET - Automatically generated secret key which encrypts the trust store and keystore password

The logging can be configured using the log4j.properties file in the conf folder.

## Event driven model
TinyMFT provides an event driven model wherein on events are generated during the lifecycle of a file transfer. A file transfer goes through the following stages in TinyMFT:
* STARTED - When the file transfer starts
* INPROGRESS - When the file is in the process of being uploaded or downloaded
* ENDED - When a file transfer successfully completes
* ABORTED - When a an error occurs during file transfer

For each of these stages TinyMFT sends out an event in xml format to the seda:transferevent camel route. A sample route has been provided which ingests this event and copies the file to a separate location. Using the apache camel you can specify conditional processing. It is recommend that for each specific processing a differnt route is created which is fed conditonally by the route catching the message from seda:transferevent.

A sample event looks something like this:
```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Event>
    <id>51104791-69f3-4aa7-a420-afc7d41b137c</id>
    <date>2018-09-30T18:45:02.061-07:00</date>
    <lastevent></lastevent>
    <properties>
        <entry>
            <key>file</key>
            <value>/opt/test/FTPS.jmx</value>
        </entry>
        <entry>
            <key>filename</key>
            <value>/FTPS.jmx</value>
        </entry>
        <entry>
            <key>session</key>
            <value>sEWBjSZ+kiA+y335vsn4SP6MOajNQM7O/PpO8hrIVcw=</value>
        </entry>
        <entry>
            <key>action</key>
            <value>UPLOAD</value>
        </entry>
        <entry>
            <key>id</key>
            <value>f939ffef7306897e0006d3d5420f4399</value>
        </entry>
        <entry>
            <key>transmittedbytes</key>
            <value>4551</value>
        </entry>
        <entry>
            <key>filesize</key>
            <value>4551</value>
        </entry>
        <entry>
            <key>state</key>
            <value>ENDED</value>
        </entry>
        <entry>
            <key>username</key>
            <value>test</value>
        </entry>
    </properties>
</Event>
```
