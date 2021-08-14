# scrypt-bitcoin4j-demos
Example Kotlin Tests to Demo sCrypt Integration

## Description
This project contains example code in the form of Unit Tests that demonstrates 
how one would integrate sCrypt smart contracts using the Bitcoin4J library. 

## Running
To run the tests:

```sh
./gradlew test
```

## Code Organisation
```sh
├── scrypt
│   ├── pushTxCounter.scrypt
│   ├── pushTxTest.scrypt
│   └── sometest.scrypt
└── src
    ├── main
    │   ├── java
    │   ├── kotlin
    │   │   └── org
    │   │       └── twostack
    │   │           └── scrypt
    │   │               └── dto
    │   │                   └── ScryptContractDto.kt
    │   └── resources
    └── test
        ├── kotlin
        │   └── org
        │       └── twostack
        │           └── scrypt
        │               └── ScryptContractTest.kt
        └── resources
            └── contracts
                ├── pushTxCounter_release_desc.json
                ├── pushTxTest_release_desc.json
                └── sometest_release_desc.json
                
```
