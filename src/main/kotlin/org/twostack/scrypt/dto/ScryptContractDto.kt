package org.twostack.scrypt.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * JSON Serialization for sCrypt Smart Contract Descriptor
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class ContractParam(
    val name: String,
    val type: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ContractAbi(
    val type: String,
    val name: String?,
    val index: Int?,
    val params: List<ContractParam>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ScryptContractDto(
    val version : Int,
    val compilerVersion : String,
    val contract : String,
    val md5 : String,
    val abi : List<ContractAbi>,
    val buildType : String,
    val asm : String

)