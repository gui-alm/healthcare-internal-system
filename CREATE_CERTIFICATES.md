# CREATING A SELF-SIGNED SSL CERTIFICATE

To add an IP address as a Subject Alternative Name (SAN) to a self-signed certificate in a Java KeyStore (JKS) generated with keytool, you'll need to follow a few steps. 

Please note that adding IP addresses to SAN is not a common practice for SSL certificates, as typically domain names are used. However, it can be done for specific use cases.

## Step 1: Create a Configuration File

Create a configuration file (let's call it san_config.cnf) with the following content:

```
distinguished_name  = req_distinguished_name
req_extensions      = v3_req

[req_distinguished_name]
[ v3_req ]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
subjectAltName = @alt_names

[alt_names]
IP.1 = 192.168.1.1
```

Replace "192.168.1.1" with the actual IP address you want to include in the SAN (your API server's IP).

## Step 2: Generate the Key Pair and CSR
Generate the key pair and the certificate signing request (CSR) using keytool:

```
keytool -genkey -keyalg RSA -alias your_alias -keystore keystore.jks -keysize 2048
```

```
keytool -certreq -alias your_alias -keystore keystore.jks -file your_csr.csr
```

## Step 3: Sign the CSR with the Configuration File

Use OpenSSL to sign the CSR with the SAN entries specified in the configuration file:

```
openssl x509 -req -in your_csr.csr -out your_signed_certificate.pem -signkey keystore.jks -days 365 -extfile san_config.cnf -extensions v3_req
```

## Step 4: Import the Signed Certificate into the Keystore
Import the signed certificate back into the keystore:

```
keytool -import -alias your_alias -file your_signed_certificate.pem -keystore keystore.jks
```


## Step 5: Verify the Certificate
Verify that the certificate now includes the IP address:

```
keytool -list -v -keystore keystore.jks -alias your_alias
```

Look for the "Subject Alternative Name" field in the output, and it should contain the specified IP address.

Keep in mind that using IP addresses in SSL certificates might not be the best practice, and it's crucial to understand the implications. In many cases, it's recommended to use domain names instead.

## Now, all you have to do is change some fields in the `application.properties` file

Example:

```
server.ssl.key-store=classpath:your_keystore_file_path.jks
server.ssl.key-store-password=your_password
server.ssl.key-password=your_password
```

Send your keystore.jks file to the client so it can use it as well. Don't forget to change the password on the `Client.java` file in line 223 to the password you just selected.