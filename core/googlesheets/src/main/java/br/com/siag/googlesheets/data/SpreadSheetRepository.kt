package br.com.siag.googlesheets.data

import br.com.siag.googlesheets.data.handle_response.OutCome
import br.com.siag.googlesheets.data.handle_response.safeApiCall
import br.com.siag.googlesheets.data.mapper.toPlanilhas
import br.com.siag.googlesheets.data.model.Planilha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

interface SpreadSheetRepository {
    suspend fun getSpreadSheet(): Flow<OutCome<List<Planilha>>>
}

class SpreadSheetRepositoryImpl @Inject constructor(
    private val spreadSheetsApi: SheetsApi
) : SpreadSheetRepository {
    override suspend fun getSpreadSheet(): Flow<OutCome<List<Planilha>>> {
        return flowOf(
            safeApiCall(Dispatchers.IO) {
                spreadSheetsApi.getSpreadSheetById().toPlanilhas()
            }
        )
    }
}