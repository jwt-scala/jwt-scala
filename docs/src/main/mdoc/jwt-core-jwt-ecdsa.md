## Jwt with ECDSA algorithms

### With generated keys

#### Generation

```scala
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.spec.{ECPrivateKeySpec, ECPublicKeySpec, ECGenParameterSpec, ECParameterSpec, ECPoint}
import java.security.{SecureRandom, KeyFactory, KeyPairGenerator, Security}
import pdi.jwt.{Jwt, JwtAlgorithm}
// We specify the curve we want to use
val ecGenSpec = new ECGenParameterSpec("P-521")
// We are going to use a ECDSA algorithm
// and the Bouncy Castle provider
if (Security.getProvider("BC") == null) {
  Security.addProvider(new BouncyCastleProvider())
}
val generatorEC = KeyPairGenerator.getInstance("ECDSA", "BC")
generatorEC.initialize(ecGenSpec, new SecureRandom())
// Generate a pair of keys, one private for encoding
// and one public for decoding
val ecKey = generatorEC.generateKeyPair()
```

#### Usage

```scala
val token = Jwt.encode("""{"user":1}""", ecKey.getPrivate, JwtAlgorithm.ES512)

Jwt.decode(token, ecKey.getPublic, JwtAlgorithm.allECDSA)
```

### With saved keys

Let's say you already have your keys, it means you know the **S** param for the private key and both **(X, Y)** for the public key. So we will first recreate the keys from those params and then use them just as we did for the previously generated keys.

#### Creation

```scala
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec

// Our saved params
val S = BigInt("1ed498eedf499e5dd12b1ab94ee03d1a722eaca3ed890630c8b25f1015dd4ec5630a02ddb603f3248a3b87c88637e147ecc7a6e2a1c2f9ff1103be74e5d42def37d", 16)
val X = BigInt("16528ac15dc4c8e0559fad628ac3ffbf5c7cfefe12d50a97c7d088cc10b408d4ab03ac0d543bde862699a74925c1f2fe7c247c00fddc1442099dfa0671fc032e10a", 16)
val Y = BigInt("b7f22b3c1322beef766cadd1a5f0363840195b7be10d9a518802d8d528e03bc164c9588c5e63f1473d05195510676008b6808508539367d2893e1aa4b7cb9f9dab", 16)

// Here we are using the P-521 curve but you need to change it
// to your own curve
val curveParams = ECNamedCurveTable.getParameterSpec("P-521")
val curveSpec: ECParameterSpec = new ECNamedCurveSpec( "P-521", curveParams.getCurve(), curveParams.getG(), curveParams.getN(), curveParams.getH());

val privateSpec = new ECPrivateKeySpec(S.underlying(), curveSpec)
val publicSpec = new ECPublicKeySpec(new ECPoint(X.underlying(), Y.underlying()), curveSpec)

val privateKeyEC = KeyFactory.getInstance("ECDSA", "BC").generatePrivate(privateSpec)
val publicKeyEC = KeyFactory.getInstance("ECDSA", "BC").generatePublic(publicSpec)
```

#### Usage

```scala
val token = Jwt.encode("""{"user":1}""", privateKeyEC, JwtAlgorithm.ES512)

Jwt.decode(token, publicKeyEC, Seq(JwtAlgorithm.ES512))

// Wrong key...
Jwt.decode(token, ecKey.getPublic, Seq(JwtAlgorithm.ES512))
```
