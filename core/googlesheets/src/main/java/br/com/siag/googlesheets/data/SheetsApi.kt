package br.com.siag.googlesheets.data

import br.com.siag.googlesheets.data.model.SpreadSheet
import retrofit2.http.GET
import retrofit2.http.Query

interface SheetsApi {
    @GET("$SHEET_ID/values/Página1!A1:C500")
    suspend fun getSpreadSheetById(
        @Query("key") key: String = "AIzaSyDtethmQyVt2f4Lu1O9RExEcpGJFG8LnkY"
    ): SpreadSheet

    companion object {
        private const val SHEET_ID = "1xKi66ePk8RXExuCuZSfKx4fIvgAknl2FCZWrpmzhlYc"
    }
}