package client

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Status
import org.http4s.circe.{jsonOf, _}
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.dsl.io.{POST, uri}

abstract class PasswordClient[F[_]](httpClient: Client[F]) {
  def requestToken(userName: String): F[Token]

  def requestPassword(token: Token): F[EncryptedPassword]

  def validatePassword(token: Token, encryptedPassword: String, decryptedPassword: String): F[Status]
}

object PasswordClient {

  def getPassword[F[+_]](client: PasswordClient[F], token: Token): F[Password] = {
    client.requestPassword(token)
  }

  def create(httpClient: Client[IO]): PasswordClient[IO] =
    new PasswordClient[IO](httpClient) {
      override def requestToken(userName: String): IO[Token] = {
        val req = POST(uri("http://localhost:9000/register"), User(userName).asJson)
        httpClient.expect(req)(jsonOf[IO, Token])
      }

      override def requestPassword(token: Token): IO[EncryptedPassword] = {
        val req = POST(uri("http://localhost:9000/send-encrypted-password"), token.asJson)
        httpClient.expect(req)(jsonOf[IO, EncryptedPassword])
      }

      override def validatePassword(token: Token, encryptedPassword: String, decryptedPassword: String): IO[Status] = {
        val result = ValidatePassword(token.token, encryptedPassword, decryptedPassword)
        val req = POST(uri("http://localhost:9000/validate"), result.asJson)
        httpClient.status(req)
      }
    }
}