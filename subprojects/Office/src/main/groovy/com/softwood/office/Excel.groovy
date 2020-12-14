package com.softwood.office


import org.apache.poi.ss.usermodel.*
import org.apache.poi.hssf.usermodel.*
import org.apache.poi.xssf.usermodel.*
import org.apache.poi.ss.util.*
import org.apache.poi.ss.usermodel.*
import java.io.*

//see http://poi.apache.org/spreadsheet/quick-guide.html#Iterator
class Excel {

    def parse(String path, sheetIndex=0) {
        if (!path.contains(".xlsx")) {
            path.concat(".xlsx")
        }

        InputStream inp = new FileInputStream(path)
        Workbook wb = WorkbookFactory.create(inp)
        Sheet sheet = wb.getSheetAt(sheetIndex)

        Iterator<Row> rowIt = sheet.rowIterator()
        Row row = rowIt.next()
        def headers = getRowData(row)

        def rows = []
        while(rowIt.hasNext()) {
            row = rowIt.next()
            rows << getRowData(row)
        }
        [headers, rows]
    }

    def getRowData(Row row) {
        def data = []
        for (Cell cell : row) {
            getValue(row, cell, data)
        }
        data
    }

    def getCellValue (Row row, Cell cell) {
        def rowIndex = row.getRowNum()
        def colIndex = cell.getColumnIndex()

        def value = ""
        switch (cell.getCellType()) {
            case CellType.STRING:
                value = cell.getRichStringCellValue().getString()
                break

            case CellType.NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue()
                } else {
                    value = cell.getNumericCellValue()
                }
                break

            case CellType.BOOLEAN:
                value = cell.getBooleanCellValue()
                break

            case CellType.FORMULA:
                value = cell.getCellFormula()
                break

            default:
                value = "<unknown sell type>"
        }

        value
    }

    def getRowReference(Row row, Cell cell) {
        def rowIndex = row.getRowNum()
        def colIndex = cell.getColumnIndex()
        CellReference ref = new CellReference(rowIndex, colIndex)
        ref.formatAsString()
        /* switch (cell.getCellType()) {
            case CellType.STRING:
                cell.getRichStringCellValue().getString()
                break

            case CellType.NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.getDateCellValue()
                } else {
                    cell.getNumericCellValue()
                }
                break

            case CellType.BOOLEAN:
                cell.getBooleanCellValue()
                break

            case CellType.FORMULA:
                cell.getCellFormula()
                break

            case CellType.BLANK:
                "BLANK"
                break
        }
        ref.getRichStringCellValue().getString()
        */

    }

    def getValue(Row row, Cell cell, List data) {
        def rowIndex = row.getRowNum()
        def colIndex = cell.getColumnIndex()
        def value = ""
        switch (cell.getCellType()) {
            case CellType.STRING:
                value = cell.getRichStringCellValue().getString();
                break

            case CellType.NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue()
                } else {
                    value = cell.getNumericCellValue()
                }
                break

            case CellType.BOOLEAN:
                value = cell.getBooleanCellValue()
                break

            case CellType.FORMULA:
                value = cell.getCellFormula()
                break

            default:
                value = ""
        }
        data[colIndex] = value
        data
    }

    def toXml(header, row) {
        def obj = "<object>\n"
        row.eachWithIndex { datum, i ->
            def headerName = header[i]
            obj += "\t<$headerName>$datum</$headerName>\n"
        }
        obj += "</object>"
    }

}
