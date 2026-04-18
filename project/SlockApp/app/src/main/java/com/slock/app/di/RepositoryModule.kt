package com.slock.app.di

import com.slock.app.data.repository.AgentRepository
import com.slock.app.data.repository.AgentRepositoryImpl
import com.slock.app.data.repository.AuthRepository
import com.slock.app.data.repository.AuthRepositoryImpl
import com.slock.app.data.repository.BillingRepository
import com.slock.app.data.repository.BillingRepositoryImpl
import com.slock.app.data.repository.ChannelRepository
import com.slock.app.data.repository.ChannelRepositoryImpl
import com.slock.app.data.repository.MachineRepository
import com.slock.app.data.repository.MachineRepositoryImpl
import com.slock.app.data.repository.MessageRepository
import com.slock.app.data.repository.MessageRepositoryImpl
import com.slock.app.data.repository.ServerRepository
import com.slock.app.data.repository.ServerRepositoryImpl
import com.slock.app.data.repository.TaskRepository
import com.slock.app.data.repository.TaskRepositoryImpl
import com.slock.app.data.repository.ThreadRepository
import com.slock.app.data.repository.ThreadRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    @Singleton
    abstract fun bindAgentRepository(impl: AgentRepositoryImpl): AgentRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindThreadRepository(impl: ThreadRepositoryImpl): ThreadRepository

    @Binds
    @Singleton
    abstract fun bindMachineRepository(impl: MachineRepositoryImpl): MachineRepository
}
