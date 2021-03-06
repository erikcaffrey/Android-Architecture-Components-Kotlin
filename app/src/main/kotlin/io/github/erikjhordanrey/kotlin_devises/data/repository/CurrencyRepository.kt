/**
 * Copyright 2017 Erik Jhordan Rey.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.erikjhordanrey.kotlin_devises.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.erikjhordanrey.kotlin_devises.data.remote.CurrencyResponse
import io.github.erikjhordanrey.kotlin_devises.data.remote.RemoteCurrencyDataSource
import io.github.erikjhordanrey.kotlin_devises.data.room.CurrencyEntity
import io.github.erikjhordanrey.kotlin_devises.data.room.LocalCurrencyDataSource
import io.github.erikjhordanrey.kotlin_devises.domain.AvailableExchange
import io.github.erikjhordanrey.kotlin_devises.domain.Currency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CurrencyRepository constructor(private val localCurrencyDataSource: LocalCurrencyDataSource,
                                     private val remoteCurrencyDataSource: RemoteCurrencyDataSource) : Repository {

    val allCompositeDisposable: MutableList<Disposable> = arrayListOf()

    override fun getTotalCurrencies() = localCurrencyDataSource.getCurrenciesTotal()

    override fun addCurrencies() {
        localCurrencyDataSource.insertCurrencies()
    }

    override fun getCurrencyList(): LiveData<List<Currency>> {
        val mutableLiveData = MutableLiveData<List<Currency>>()
        val disposable = localCurrencyDataSource.getAllCurrencies()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ currencyList ->
                    mutableLiveData.value = transform(currencyList)
                }, { t: Throwable? -> t?.printStackTrace() })
        allCompositeDisposable.add(disposable)
        return mutableLiveData
    }

    private fun transform(currencies: List<CurrencyEntity>) = mutableListOf<Currency>().apply {
        currencies.forEach { add(Currency(it.countryCode, it.countryName)) }
    }

    override fun getAvailableExchange(currencies: String): LiveData<AvailableExchange> {
        val mutableLiveData = MutableLiveData<AvailableExchange>()
        val disposable = remoteCurrencyDataSource.requestAvailableExchange(currencies)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.isSuccess) mutableLiveData.value = transform(it)
                    else throw Throwable("CurrencyRepository -> on Error occurred")

                }, { throwable: Throwable? -> throwable?.printStackTrace() })
        allCompositeDisposable.add(disposable)
        return mutableLiveData
    }

    private fun transform(exchangeMap: CurrencyResponse): AvailableExchange {
        return AvailableExchange(exchangeMap.currencyQuotes)
    }
}
