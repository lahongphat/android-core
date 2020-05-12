package com.support.core.phone

import android.content.Context
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import androidx.annotation.WorkerThread
import com.support.core.extension.sub
import com.support.core.functional.Parser
import com.support.core.helpers.PhoneUtils
import java.util.*
import kotlin.collections.HashMap

class PhoneNumberFormatter : PhoneNumberFormattingTextWatcher() {
    private val plusReg = Regex("\\+")

    override fun afterTextChanged(s: Editable) {
        val string = s.toString()
        if (string.contains(plusReg)) {
            s.replace(0, s.length, string.replace(plusReg, ""))
        }
        super.afterTextChanged(s)
    }
}

class PhoneCodeProvider(private val context: Context,
                        private val path: String,
                        private val parser: Parser) {

    private var mFinder: PhoneCodeFinder? = null

    val finder: PhoneCodeFinder
        get() {
            if (mFinder == null) {
                val data = parser.fromJson(
                        context.assets.open(path).bufferedReader().readText(),
                        PhoneCodeData::class.java
                )?.data ?: error("Can not parse")
                mFinder = PhoneCodeFinder(data)
            }
            return mFinder!!
        }

    @WorkerThread
    fun gets(): List<IPhoneCode> {
        return finder.data
    }

    fun createPhone(rawPhone: String, countryCode: () -> String): PhoneNumber {
        val finder = finder
        return when {
            rawPhone.isBlank() -> EmptyPhoneNumber()
            rawPhone.first() == '+' -> {
                val code = finder.findByPhone(rawPhone)
                return PhoneNumber(code, rawPhone.removePrefix(code.dialCode))
            }
            rawPhone.first() == '0' -> {
                val code = finder.findByCountryCode(countryCode())
                return PhoneNumber(code, rawPhone.removePrefix("0"))
            }
            else -> {
                val code = finder.findByCountryCode(countryCode())
                return PhoneNumber(code, rawPhone)
            }
        }
    }

    fun get(countryCode: String): IPhoneCode {
        return finder.findByCountryCode(countryCode)
    }
}

class PhoneCode(
        override val name: String,
        private val dial_code: String,
        override val code: String
) : IPhoneCode {
    override val dialCode: String
        get() = dial_code
}

class EmptyPhoneCode : IPhoneCode {
    override val name: String
        get() = ""
    override val dialCode: String
        get() = ""
    override val code: String
        get() = ""
}

class PhoneCodeData(val data: List<PhoneCode>)

class PhoneCodeFinder(val data: List<PhoneCode>) {

    private val mDials = HashMap<String, IPhoneCode>()
    private val mCountries = HashMap<String, IPhoneCode>()

    init {
        data.forEach {
            mDials[it.dialCode] = it
            mCountries[it.code] = it
        }
    }

    fun findByPhone(rawPhone: String): IPhoneCode {
        for (i in (3..5)) {
            val code = rawPhone.sub(0, i)
            if (mDials.containsKey(code)) return mDials[code]!!
        }
        return data.first()
    }

    fun findByCountryCode(countryCode: String): IPhoneCode {
        return mCountries[countryCode.toUpperCase(Locale.ROOT)]!!
    }
}

open class PhoneNumber(val code: IPhoneCode, val body: String) {
    val isValid: Boolean get() = PhoneUtils.isValid(formattedValue)
    val isBlank: Boolean get() = body.isBlank()

    val formattedValue get() = "${code.dialCode} $body"

    val value get() = "${code.dialCode}${PhoneUtils.getPhone(body)}"
}

class EmptyPhoneNumber : PhoneNumber(PhoneCode("", "", ""), "")

interface IPhoneCode {
    val name: String
    val dialCode: String
    val code: String
}