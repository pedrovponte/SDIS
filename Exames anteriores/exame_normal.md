# Exame Normal

[Source](https://drive.google.com/drive/folders/10U7yrKyXGFKsK-D49BC6b2tqm0ZLJ-UH)

**1.** C ou E

**2.** C

**3.** True

**4.** False

**5.** True

**6.** B

**7.** False

**8.** True

**9.** A

**10.** False

**11.** 

**a)** A Alice pretende falar com o Bob. Para isso, envia a mensagem 1. Como resposta (mensagem 2), o KDC gera a shared key K<sub>A,B</sub> que é encriptada com a private key K<sub>A,KDC</sub> que a Alice partilha com o KDC. Esta mensagem é também enviada para o Bob, para que este tenha acesso à shared key.

**b)** Replay Attack - Consider Chuck has stolen one of Bob’s old keys by intercepting  an old response that the KDC has returned from previous conversation from Alice and Bob.

Chuck waits for the next request from Alice to set up a conversation with Bob, at this time he replays the old intercepted response as he knows the old key from the previous conversation.This way she can fool Alice into thinking she is Bob.

To avoid this we can add a nonce the content of the shared key, a nonce is a very large  random number chosen from a set with the intent of correlating two messages to each other.

**c)** Needham-Schroeder’s Protocol

**12.** D

**13.** True

**14.** A

**15.** True

**16.** D

**17.** B

**18.** True

**19.** D

**20.** E

**22.** A
