package pdi.jwt

/** A JWK Set (RFC 7517 section 5). */
final case class JwkSet(keys: Seq[Jwk]) {

  /** The first key of the set with this `kid`, even when other keys carry the same `kid`, see
    * [[findAll]]. Keys without a `kid` never match.
    */
  def find(keyId: String): Option[Jwk] = keys.find(_.keyId.contains(keyId))

  /** All the keys of the set with this `kid`, in the order of the set. As kid values need not be
    * unique in a set (RFC 7517 section 4.5), the caller selects among them with the other members.
    * Keys without a `kid` never match.
    */
  def findAll(keyId: String): List[Jwk] = keys.filter(_.keyId.contains(keyId)).toList

  def toJson: String = "{\"keys\":" + keys.map(_.toJson).mkString("[", ",", "]") + "}"
}
