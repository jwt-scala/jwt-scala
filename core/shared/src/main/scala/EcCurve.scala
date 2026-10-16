package pdi.jwt

import java.security.AlgorithmParameters
import java.security.spec.{ECGenParameterSpec, ECParameterSpec}

/** An elliptic curve usable in an `EC` JWK (RFC 7518 section 6.2.1.1). */
sealed trait EcCurve {

  /** The JOSE `crv` value, e.g. `P-256`. */
  def name: String

  /** The JDK name of the curve, e.g. `secp256r1`. */
  def jdkName: String

  /** The fixed size in bytes of the `x` and `y` coordinates. */
  def coordinateLength: Int

  private[jwt] def parameterSpec: ECParameterSpec = EcCurve.specs(this)
}

object EcCurve {
  case object P256 extends EcCurve {
    def name = "P-256"; def jdkName = "secp256r1"; def coordinateLength = 32
  }
  case object P384 extends EcCurve {
    def name = "P-384"; def jdkName = "secp384r1"; def coordinateLength = 48
  }
  case object P521 extends EcCurve {
    def name = "P-521"; def jdkName = "secp521r1"; def coordinateLength = 66
  }

  def all(): Seq[EcCurve] = Seq(P256, P384, P521)

  def fromName(name: String): Option[EcCurve] = all().find(_.name == name)

  private lazy val specs: Map[EcCurve, ECParameterSpec] = all().map { curve =>
    val parameters = AlgorithmParameters.getInstance("EC")
    parameters.init(new ECGenParameterSpec(curve.jdkName))
    curve -> parameters.getParameterSpec(classOf[ECParameterSpec])
  }.toMap

  private[jwt] def of(params: ECParameterSpec): Option[EcCurve] = all().find { curve =>
    val spec = curve.parameterSpec
    spec.getCurve == params.getCurve &&
    spec.getGenerator == params.getGenerator &&
    spec.getOrder == params.getOrder &&
    spec.getCofactor == params.getCofactor
  }
}
