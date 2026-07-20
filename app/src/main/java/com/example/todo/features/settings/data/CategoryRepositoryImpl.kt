package com.example.todo.features.settings.data

import com.example.todo.core.database.dao.CategoryDao
import com.example.todo.core.database.entity.CategoryEntity
import com.example.todo.features.settings.domain.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeAllCategories()

    override suspend fun getAllCategories(): List<CategoryEntity> =
        categoryDao.getAllCategories()

    override suspend fun getCategoryById(id: String): CategoryEntity? =
        categoryDao.getCategoryById(id)

    override suspend fun insertCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    override suspend fun deleteAll() =
        categoryDao.deleteAll()

    override suspend fun insertAll(categories: List<CategoryEntity>) =
        categoryDao.insertAll(categories)
}
