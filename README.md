# Project 4

## Contributors

- [Nirespire](https://github.com/Nirespireh)
- [Preethu19th](https://github.com/preethu19th)


## Overview

This project aimed at simulating a small portion of the Facebook Graph API through the use of the Akka and Spray
Scala frameworks. Additionally, a simulator for actual users of the facebook APIs was created. 
The software was built as a foundation to eventually add secure features such as digital signatures, public-private
key exchanges, and secure hashing of all content such that not even the server would be able to read information if it was not 
intended to see it.

 
## What is Working

- Facebook REST server with CRUD support for Profile, Page, Picture, Album, Friendlist and Post objects.
- Client simulator with close to real-life activity patterns based on different behavior profiles.


## How to Run

To run the program, simply execute `sbt run`.
This will start the facebook server backend and after a short pause, the client simulator will start as well.
The number of clients that will be simulated can be adjusted in the `application.conf` file in the resources folder.
The percentage of clients that behave in certain ways with respect to how they use the server to perform facebook
actions will remain the same no matter the number of clients specified.


## Implementation Details

The Facebook REST API server is built on the spray-can framework built on top of Akka. Each time a user performs a Put
on the endpoint for the Profile objects (Page and User), it corresponds to the server creating a new Profile Actor responsible for handling 
all the requests sent with regard to that user. This means every time that user creates any object, that actor will be delegated to for handling
the request. Similiarly, if another user tries to get any objects from a user, that user's actor will be responsible for completing the request.

A pool of Delegator Actors is responsible for accepting the initial connections from clients and forwarding requests to their appropriate 
Profile Actors. All objects are stored in memory and there is no database layer present. A single Debug Actor is responsible for a single "/debug" endpoint
and is used to keep track of various runtime metrics of the server such as average number of requests completed per second and total number of objects
created. It provides a way to periodically probe the server and get a snapshot of its status.

The Client Simulator is implemented as a seperate actor system within the same program that starts up after a short delay once the server system
has been initialized. Each Client actor registers itself with the server as a Profile in the form of either a User or a Page. After another short delay, each
Client actor begins a process by which every second they perform one or many requests to the server depending on the behavior they have been assigned to.
Additionally, there is a single Matchmaker Actor responsible for simulating Clients meeting each other in real life and subsequently creating a connection
within the facebook server.

## Security

![Objects](/objects.PNG?raw=true "Objects")

The scheme for securely publishing content begins with the prerequisite of every client and the server having a unique public-private key pair with which they can securely encrypt and send content to a single party and only that single part will be able to read the content. The client begins by creating a Facebook object similarly to the previous project implementation, while also generating a 128 bit AES key through secure random number generation. The client encrypts the created object into SecureObject which contains basic metadata about the object like its type and owner ID as well as the original object encrypted via AES128 and represented as a base 64 encoded string. Since the client should have total control over what other clients can read their newly created object, they are responsible for encrypting the AES key with every one of their friends’ public RSA keys. The Client then constructs a SecureMessage object around the SecureObject by encrypting the SecureObject with the server’s public key and signing the message with its own private key. Finally, the client sends the SecureMessage as JSON to the server. The server is responsible for validating the signature of the SecureMessage to ensure that the sender is genuine, decrypting the SecureMessage with its own private key to retrieve the SecureObject, and storing the object and its various encrypted AES keys in appropriate location according to the user that published it. Nowhere in this process does any information about the original Facebook become available to see by the server or any party not authorized by the creator to view the content.

![Publishing Objects](/publishing.PNG?raw=true "Publishing Objects")

Object retrieval from the Client begins with them constructing a SecureRequest object, filling in the appropriate fields, and constructing a SecureMessage around it similarly to the process used for SecureObjects for publishing. Object retrieval from the server’s perspective requires four main steps. First the SecureMessage from the Client must be validated by its signature. Second, the appropriate SecureObject must be retrieved by its ID and type which is known to the server. Third, the appropriate encrypted AES key for the SecureObject for that specific client needs to be retrieved. Since there will be multiple encrypted AES keys for a single object, the server needs to keep track of what keys belong to what client-object combination. Lastly, the server will retrieve the requesting client’s public key that was stored upon client registration, encrypt the SecureObject and encrypted AES key, construct a SecureMessage around it, and send it over to the client. The client must then decrypt the SecureMessage with its private key, decrypt the AES key from the SecureObject with its private key, and finally decrypt the SecureObject contents with the AES key.

![Retrieving Objects](/retrieving.PNG?raw=true "Retrieving Objects")

## Facebook Graph API Components Referenced

- Page: https://developers.facebook.com/docs/graph-api/reference/v2.5/page
    /page/{page-id}
- Post: https://developers.facebook.com/docs/graph-api/reference/v2.5/post
    /post/{post-id}
- Friend List: https://developers.facebook.com/docs/graph-api/reference/v2.5/friendlist
    /friendlist/{friendlist-id}
- Profile: https://developers.facebook.com/docs/graph-api/reference/v2.5/profile
    /profile/{profile-id}
- Album: https://developers.facebook.com/docs/graph-api/reference/v2.5/album
    /album/{album-id}
- Picture: https://developers.facebook.com/docs/graph-api/reference/v2.5/album/picture
    /album/{album-id}/{picture-id}

#### References for designing user simulator

http://www.sciencedirect.com/science/article/pii/S0747563211000379

Tracii Ryan, Sophia Xenos, Who uses Facebook? An investigation into the relationship between the Big Five, shyness, narcissism, loneliness, and Facebook usage, Computers in Human Behavior, Volume 27, Issue 5, September 2011, Pages 1658-1664, ISSN 0747-5632, http://dx.doi.org/10.1016/j.chb.2011.02.004.
(http://www.sciencedirect.com/science/article/pii/S0747563211000379)
Keywords: Facebook; Big Five; Personality; Narcissism; Shyness; Loneliness
