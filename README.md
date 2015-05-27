# notary-cache

## Requirements
NotaryCache uses maven2 to build the download all requirements and to build the executables. Moreover, Java 8 is required. Java can also be provided in the same directory (see later) and must not be installed in the system. 

## Install 
$ mvn package appassembler:assemble

$ mv target/natives target/appassembler/lib

The appassembler directory now contains both the dependencies and the required native libraries. It can be zipped and transferred to an arbitrary system.

## Run
$ cd target/appassembler

$ ./bin/NotaryCache.sh

If no default Java JRE is available, make sure to use the following command under Unix OSes, which assumes a Java runtime in the root directory of NotaryCache:

$ PATH=./jdk1.8.0_40/bin/:$PATH ./bin/NotaryCache.sh

## Operation
Events can be send to the system both by modules and manually by the system administrator. However, it is proposed, that only the following events are send, as they are used for debugging and administration purposes only.
- get-cache-info - Shows information about the current version of the cache
- config-reload - reloads the configuration file
- config-generatePK - renews the cryptographic material
- set-keysize - changes the keysize of the cryptographic material
- quit - Quits NotaryCache
- hwmon-change-polling [number] - changes the period to poll various hardware parameters
- get-cache-full - Returns the cache without header and signature to STDOUT
- clear-cache - Clears the cache
- set-cache-size - Sets the maximum size of the cache

NotaryCache is by default not accessible from outside the system. To make NotaryCache communicate with the rest of the network, one has to install and configure a widely acknowledged web server implementation, such as nginx. The following configuration has to be done in nginx:
  
	server {
	  listen   80; ## listen for ipv4; this line is default and implied
	  
	  root /srv/www/myweb; ## example
	  index index.html; ## example
	  server_name somehost.tld; ## example
	  
	  ## This part is fixed and must be configured as given
	  location /.well-known/ {
	  	  proxy_pass http://localhost:8081;
	  }
	}

It is further proposed to implement SSL. For this implementation, refer to the respectice manual, for example:
- Apache: http://wiki.ubuntuusers.de/Apache/SSL
- nginx: https://www.digicert.com/ssl-certificate-installation-nginx.htm
- ...


## Notes
Windows: ./bin/NotaryCache.bat


