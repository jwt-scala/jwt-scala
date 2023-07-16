package pdi.jwt

trait JwtCorePlatform[H, C] extends JwtCoreFunctions { self: JwtCore[H, C] => }
