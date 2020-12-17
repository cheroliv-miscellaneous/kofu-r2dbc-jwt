package kofu

import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.empty
import org.springframework.data.relational.core.query.Query.query

class UserRepository(private val operations: R2dbcEntityOperations) {
    companion object {
        @JvmStatic
        private val userDataModel: Class<User> by lazy { User::class.java }
    }

    fun count() = operations.count(empty(), userDataModel)


    fun findAll() = operations.select(empty(), userDataModel)


    fun findOne(id: String?) =
        operations.select(userDataModel)
            .matching(
                query(
                    where("login")
                        .`is`(id!!)
                )
            )
            .one()


    fun deleteAll() =
        operations.delete(userDataModel)
            .all().then()


    fun insert(user: User) =
        operations.insert(userDataModel)
            .using(user)
}