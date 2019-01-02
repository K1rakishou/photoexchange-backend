package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.core.FirebaseToken
import com.kirakishou.photoexchange.core.User
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.dao.UsersDao
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.CoroutineDispatcher

open class UsersRepository(
  private val usersDao: UsersDao,
  private val generator: GeneratorService,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(dispatcher) {

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

  suspend fun createNew(): User {
    return dbQuery(User.empty()) {
      val userUuid = generateUserUuid()

      return@dbQuery usersDao.save(userUuid).toUser()
    }
  }

  open suspend fun accountExists(userUuid: UserUuid): Boolean {
    return dbQuery {
      return@dbQuery usersDao.userExists(userUuid)
    }
  }

  open suspend fun getFirebaseToken(userUuid: UserUuid): FirebaseToken {
    return dbQuery(FirebaseToken.empty()) {
      return@dbQuery usersDao.getUser(userUuid).firebaseToken
    }
  }

  suspend fun updateFirebaseToken(userUuid: UserUuid, newToken: FirebaseToken): Boolean {
    return dbQuery(false) {
      return@dbQuery usersDao.updateFirebaseToken(userUuid, newToken)
    }
  }
}