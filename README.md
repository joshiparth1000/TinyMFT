# TinyMFT
TinyMFT is a multi-protocol file transfer gateway. It supports the following protocols:
* HTTPS - [Powered by Jetty](https://www.eclipse.org/jetty)
* FTPS - [Powered by Apache Mina](https://mina.apache.org/ftpserver-project/index.html)
* SFTP - [Powered by Apache Mina](https://mina.apache.org/sshd-project/index.html)

TinyMFT supports dual authentication using password and certificate/key cobmination. The authentication is pluggable and utlizes [JAAS](https://en.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service) so that any kind of user store can be utlized. The sample plugin reads credentials from a flat file and authenticates the user either using password or certificate/password or both.

Additionally, [Apache Camel](http://camel.apache.org/) provides the integration engine which lets you take care of complicated routing and transformation of files.

## Configuration
* JAASCONFIG - Specifies the location of JAAS config file
* JAASDOMAIN - JAAS domain to be used for authentication
