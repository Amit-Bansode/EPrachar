package com.chordz.eprachar.data.response

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

@Parcelize
data class ElectionMessageResponse(

	@field:SerializedName("code")
	val code: Int? = null,

	@field:SerializedName("data")
	val data: List<DataItem?>? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("status")
	val status: String? = null
) : Parcelable

@Parcelize
data class DataItem(

	@field:SerializedName("a_typesuperadmin")
	val aTypesuperadmin: Int? = null,

	@field:SerializedName("a_message")
	val aMessage: String? = null,

	@field:SerializedName("a_id")
	val aId: Int? = null,

	@field:SerializedName("a_contactno")
	val aContactno: String? = null,

	@field:SerializedName("a_code")
	val aCode: String? = null,

	@field:SerializedName("a_typeadmin")
	val aTypeadmin: Int? = null,

	@field:SerializedName("a_username")
	val aUsername: String? = null,

	@field:SerializedName("a_password")
	val aPassword: String? = null,

	@field:SerializedName("a_confirmpassword")
	val aConfirmpassword: String? = null,

	@field:SerializedName("a_name")
	val aName: String? = null,

	@field:SerializedName("a_image")
	val aImage: String? = null,

	@field:SerializedName("org_name")
	val client: String? = null


) : Parcelable
