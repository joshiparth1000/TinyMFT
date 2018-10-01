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

For each of these stages TinyMFT sends out an event in xml format to the seda:transferevent camel route. A sample route has been provided which ingests this event and copies the file to a separate location. Using the apache camel you can specify conditional processing. It is recommend that for each specific processing a differnt route is created which is fed conditonally by the route catching the event from seda:transferevent.

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

## Integration engine
Apache Camel provides the routing and file transformation engine for TinyMFT. On start up TinyMFT starts a camel context in which all the routes located in the ROUTES folder configured in config.properties are loaded and started automatically. A folder monitor periodcally checks the ROUTE folder for any changes and based on the operation stops or starts the route. If an existing route is changed then it is stopped and the new configuration is loaded and route is restarted. A sample event has already provided which consumes the event from seda:transferevent.

Using the versatility provided by Apache Camel the file can be routed or picked up from multiple different end points like SFTP remote server, S3 bucket, LDAP servers etc. Moreover, developers can extend the capabilities of TinyMFT outside of its core.


## Pluggable authentication
TinyMFT is uses JAAS to provide authentication for users. Since, JAAS is authentication agnostic TinyMFT can be integrated with any kind of user store. A sample module AuthFileLoginModule has been implemented which reads user credentials from a flat file. When implementing your own authentication module extend the AbstractLoginModule and implement the checkPasword method. In your login method you the inherited authenticate method to authenticate the user.
