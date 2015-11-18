
# OPC UA Server for the OMS
Object Memories allow physical artefacts to keep a history of their statuses, interactions and changes over a lifetime. With the growing availability of easy-to-deploy and cheap data storages such as RFID or NFC chips such a refinement of products has become comparatively affordable and possible applications are starting to arise. Whether a product's temperature is to be monitored, yielded emission, changing components or manipulations issued by other artefacts, in an object memory all relevant data can be stored and accessed at will. 

This repository provides a basic OPC UA Server which functions as an interface to the [Object Memory Server](https://github.com/dfki-iui/Digital-Object-Memories). The interface adds to the already implemented ones in the original project and makes object memories available within industrial contexts that already use the OPC UA protocol. For a more detailed description of the Object Memory Model, please refer to the [W3C Object Memory Modeling Incubator Group](http://www.w3.org/2005/Incubator/omm/) or the original repository linked above. For information about OPC UA, please refer to the [OPC Foundation's website](https://opcfoundation.org/).

---

1 [How to use this code](#ch1)

2 [How to use the server](#ch2)

---

## 1 <a name="ch1"> How to use this code </a>
The repository contains all necessary files to start and run an OPC UA Server mirroring the OMS, provided the requirements are fulfilled (see 1.1). The class `de.dfki.opcua.server.ServerStarter` can be executed to start the server which then runs on `opc.tcp://localhost:52521` by default until it is stopped manually or automatically. 

### 1.1 Requirements
Not all libraries necessary to run the server are contained in this repository or its Maven dependency list. Two essential libraries are not included due to licensing limitations that prohibit their distribution, more specifically these are the Prosys OPC UA Java SDK and the corresponding OPC UA stack. If you are interested in the project but do not have a license for these libraries you can request an evaluation license via the [Prosys website](https://prosysopc.com/products/opc-ua-java-sdk/). 

Furthermore, [the original OMM project](https://github.com/dfki-iui/Digital-Object-Memories) has to be available to the OPC UA server in order for it to work properly, so make sure that both **libomm** and **oms** are available to the OPC UA project. 

All remaining dependencies can be handled by Maven automatically and are given here for the sake of completeness. 

| Library | Version| 
| :------- | :------: |
| [Apache Log4j]( https://logging.apache.org/log4j/1.2/download.html) | 1.2.17|
| [Bouncy Castle Provider]( http://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk15on) | 1.47 |
| [Bouncy Castle PKIX, CMS, EAC, TSP, PKCS, OCSP, CMP, And CRM]( http://mvnrepository.com/artifact/org.bouncycastle/bcpkix-jdk15on/1.47) | 1.47 |
| [JSON](https://github.com/douglascrockford/JSON-java) | 20141113 |
| [Restlet]( http://restlet.com/downloads/current/) | 3.0-M1 |
| [SLF4J](http://www.slf4j.org/download.html) | 1.7.10 |

The project uses the Java JDK Version 8 Update 45 or higher (downloadable at http://java.com/). 

## 2 <a name="ch2"> How to use the server </a>
The functionality provided by the OPC UA Server does not exceed that of the original OMS, it just adds another protocol layer and makes it deployable in an OPC UA context. Therefore, only issues pertaining to the server itself are treated here. Please refer to the Digital Object Memories repository for more information on how to use the OMS. 

### 2.1 Starting and running the OPC UA Server
By default, the OPC UA Server is started via the **ServerStarter** class located in `de.dfki.opcua.server`. It sets the OPC UA port to 52521 and the HTTPS port to 52444, creates a default server name ("OMS in OPC UA") and creates an instance of the class **OmsOpcUaServer** from the same package which will then expect the OMS to run on its default address "http://localhost:10082". 

You may call this class with your own settings if you prefer, using its constructors to pass different ports to it or a different address for the OMS. For example, `OmsOpcUaServer myServer = new OmsOpcUaServer(52522, 52445, "MyServer", "http://com.objectmemoryserver:1963"); ` will initialize a new server called "MyServer" that listens for OPC UA protocol on port 52522 and HTTPS on port 52445 while mirroring the Object Memory Server found under the given address. You can then start the server by calling `myServer.run();`. 

### 2.2 Using the OPC UA Server with a client
For most purposes, a custom client is recommended that fits the specific requirements given in an existing context. For example, whether machines or humans use the client in order to gather which kind of information are important aspects to consider. 

However, in order to test the OPC UA Server it might be quicker and easier to use an existing OPC UA client which does not necessarily merit its structure and capacities, but allows basic browsing and manipulation of the object memories. Various free test clients are available to download from different sources, for example Unified Automation's [UaExpert]( https://www.unified-automation.com/products/development-tools/uaexpert.html) which provides a graphical user interface and browsing of the server structure analogous to a file system. 

In the case that no external client may be used, a very basic text-based OPC UA client is part of the repository. It can be started using the **ClientStarter** class in the client package (which utilizes the default values) or manually by using one of the constructors provided by **OmsPocUaClient** if the server's address differs from its default. 

