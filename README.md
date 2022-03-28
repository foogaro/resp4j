# RESP for Java

A simple client to write commands using the RESP (REdis Serialization Protocol) protocol.

In Redis setting a key _name_ with value _Luigi_ has the following syntax:

```shell
> SET name Luigi
```

And in RESP gets pushed on the wire as follows:

```shell
*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$5\r\nLuigi  
```

Which means:
- The input has 3 parts 
- A string ($) of 3 characters _SET_
- A string ($) of 4 characters _name_
- And a string ($) of 5 characters _Luigi_

Simplicity.

In Java, well with this client ```resp4j```, all you have to do is the following:

```java
String[] commands = new String[]{"SET", "name", "Luigi"};

try {
    Redis.run(redis -> redis.call(commands), "localhost", 6379);
} catch (IOException | RedisError e) {
    e.printStackTrace();
    throw new RedisException(e);
}
```

And your commands will be transformed into RESP commands and pushed to the Redis server.

Done.


To know more about Redis commands, visit the following link:
- https://redis.io/commands/

