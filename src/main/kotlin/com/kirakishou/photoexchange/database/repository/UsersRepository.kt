package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.core.FirebaseToken
import com.kirakishou.photoexchange.core.User
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.dao.UsersDao
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.sql.Database

open class UsersRepository(
  private val usersDao: UsersDao,
  private val generator: GeneratorService,
  database: Database,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(database, dispatcher) {

  private fun generateUserUuid(): UserUuid {
    var userUuid = UserUuid.empty()

    while (true) {
      val generatedUserId = generator.generateUserUuid()

      if (!usersDao.userExists(generatedUserId)) {
        userUuid = generatedUserId
        break
      }
    }

    return userUuid
  }

  open suspend fun createNew(): User {
    return dbQuery(User.empty()) {
      val userUuid = generateUserUuid()

      return@dbQuery usersDao.save(userUuid).toUser()
    }
  }

  open suspend fun accountExists(userUuid: UserUuid): Boolean {
    return dbQuery {
      val exists = usersDao.userExists(userUuid)
      if (exists) {
        usersDao.updateLastLoginTimeWithCurrentTime(userUuid)
      }

      return@dbQuery exists
    }
  }

  open suspend fun getFirebaseToken(userUuid: UserUuid): FirebaseToken {
    return dbQuery(FirebaseToken.empty()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery FirebaseToken.empty()
      }

      usersDao.updateLastLoginTimeWithCurrentTime(userUuid)
      return@dbQuery user.firebaseToken
    }
  }

  open suspend fun getUserUuidByUserId(userId: UserId): UserUuid {
    return dbQuery(UserUuid.empty()) {
      val user = usersDao.getUser(userId)
      if (user.isEmpty()) {
        return@dbQuery UserUuid.empty()
      }

      usersDao.updateLastLoginTimeWithCurrentTime(userId)
      return@dbQuery user.userUuid
    }
  }

  open suspend fun getUserIdByUserUuid(userUuid: UserUuid): UserId {
    return dbQuery(UserId.empty()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery UserId.empty()
      }

      usersDao.updateLastLoginTimeWithCurrentTime(userUuid)
      return@dbQuery user.userId
    }
  }

  open suspend fun updateFirebaseToken(userUuid: UserUuid, newToken: FirebaseToken): Boolean {
    return dbQuery(false) {
      val result = usersDao.updateFirebaseToken(userUuid, newToken)
      if (result) {
        usersDao.updateLastLoginTimeWithCurrentTime(userUuid)
      }

      return@dbQuery result
    }
  }
}