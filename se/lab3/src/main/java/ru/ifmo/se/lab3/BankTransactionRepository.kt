package ru.ifmo.se.lab3

import org.springframework.data.repository.CrudRepository

interface BankTransactionRepository : CrudRepository<BankTransaction, Long>
