/**
 * doPost() function to add data to a spreadsheet.
 *
 * @param {event} e Event passed to doPost, with body
 * @returns {String/html} Html to be served
 *
 * Test URLs (adjust ID as needed):
 *   https://script.google.com/macros/s/--DEV-SCRIPT-ID--/dev
 *   https://script.google.com/macros/s/--PUB-SCRIPT-ID--/exec
 * with HTTP POST body consisting of a JSON string containing
 * {run: {RUN_DATA}, keys: [ KEY_DATA, KEY_DATA ] }
 * where RUN_DATA contains the common columns and KEY_DATA contains the key matches
 */

var id = '15sKldjTZ0YbtfihlSKzEgikCJL6hMNtKuWIIpudvPBQ'; // Spreadsheet id for responses


function testPost() {
  return ContentService.createTextOutput(JSON.stringify(handlePost(
    {run:{run:"b2c8576c-c138-4eff-b708-19a3bce3ee04"},keys:[{absolute_x:1}, {absolute_y:2}]})));
}

function testGet() {
  return doGet({parameter:{config:JSON.stringify(
    {keyboard: 'org.pocketworkstation.pckeyboard/.LatinIME'}
  )}});
}

function testGets() {
  return doGet({parameter:{config:JSON.stringify(
    {keyboard: ['org.pocketworkstation.pckeyboard/.LatinIME', 'com.android.inputmethod.latin/.LatinIME']}
  )}});
}

function doGet(e) {
  Logger.log( "get " + JSON.stringify(e) );  // view parameters
  if (e.parameter['fakePost']) {
    // to allow debugging POST requests with a simple GET
    return handlePost(JSON.parse(e.parameter['fakePost']));
  } else {
    var config = JSON.parse(e.parameter['config']);
    
    if (Array.isArray(config.keyboard)) {
      var keyboards = config.keyboard;
      var result = {};
      
      keyboards.forEach(function(keyboard) {
        config.keyboard = keyboard;
        result[keyboard] = handleGet(config);
      });
      
      return ContentService.createTextOutput(JSON.stringify(result));
    } else {
      return ContentService.createTextOutput(JSON.stringify(handleGet(config)));
    }
  }
}

function doPost(e) {
  Logger.log( "post " + JSON.stringify(e) );  // view parameters
  return handlePost(JSON.parse(e.postData.getDataAsString()));
}

function dataToSheet(data, sheet, headers) {    
  headers = headers || sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  
  if (Array.isArray(data)) {
    for (var n = 0; n < data.length; n++) {
      dataToSheet(data[n], sheet, headers);
    }
  } else {
    Logger.log(data);
    var newRow = sheet.getLastRow() + 1;
    var rowData = [];
    
    for (var colN = 0; colN < headers.length; colN++) {
      var colName = headers[colN];
      Logger.log(colName);
      rowData[colN] = data[colName] || '';
    }
    
    Logger.log(JSON.stringify(rowData));
    
    // Write new row to spreadsheet
    var newRange = sheet.getRange(newRow, 1, 1, rowData.length);
    newRange.setValues([rowData]);
  }
}

function handlePost(body) {
  var result = '';
  
  if (body == undefined) {
    result = 'No body';
  } else {
    var spreadsheet = SpreadsheetApp.openById(id);
    
    dataToSheet(body.run, spreadsheet.getSheetByName('runs'));
    dataToSheet(body.keys, spreadsheet.getSheetByName('keys'));
  }
  
  if (result == '') result = 'Ok';

  // Return result of operation
  return ContentService.createTextOutput(result);
}

function handleGet(config) {
  var spreadsheet = SpreadsheetApp.openById(id);
  var runs = spreadsheet.getSheetByName('runs');
  
  var selectedRun;
  var selectedRunRow;
  var matchScore = -1;
  
  var runsHeaders = runs.getRange(1, 1, 1, runs.getLastColumn()).getValues()[0];
  
  // find the best run, based on matching columns with the config parameter,
  // columns on the left are more important
  for (var rowN = 2; rowN <= runs.getLastRow(); rowN++) {
    var row = runs.getRange(rowN, 1, rowN, runs.getLastColumn()).getValues()[0];
    
    var score = 0;
    
    // skip run and timestamp for score calculation
    for (var colN = 2; colN < runsHeaders.length; colN++) {
      var colName = runsHeaders[colN];
      
      if (row[colN] == config[colName]) {
        score += 1 << (runsHeaders.length - colN);
      }
    }
    
    if (score >= matchScore) {
      matchScore = score;
      selectedRun = row[1];
      selectedRunRow = rowN;
    }
  }
  
  var keys = spreadsheet.getSheetByName('keys');
  var keysHeaders = keys.getRange(1, 2, 1, keys.getLastColumn() - 1).getValues()[0];
  var keyRows = keys.getLastRow();
  
  // find the relevant rows to send back
  var runIds = keys.getRange(2, 1, keyRows - 1, 1).getValues();
  
  var firstRow = -1, lastRow = -1;
  
  var keysResult = [];
  
  for (var rowN = 0; rowN < keyRows - 2; rowN++) {
    if (runIds[rowN][0] == selectedRun) {
      if (firstRow == -1) {
        firstRow = rowN;
      }
      
      lastRow = rowN;
    }
  }
  
  // build result
  var keysResult = [];
  var keyInfos = keys.getRange(firstRow + 2, 2, lastRow - firstRow + 2, keys.getLastColumn() - 1).getValues();
  for (var rowN = 0; rowN < keyInfos.length; rowN++) {
    var row = keyInfos[rowN];
    var keyInfo = {};
    
    for (var colN = 0; colN < row.length; colN++) {
      keyInfo[keysHeaders[colN]] = row[colN];
    }
    
    keysResult.push(keyInfo);
  }
  
  var runResult = {};
  var run = runs.getRange(selectedRunRow, 1, 1, runs.getLastColumn() - 1).getValues()[0];
  for (var colN = 0; colN < run.length; colN++) {
    runResult[runsHeaders[colN]] = run[colN];
  }
  
  // Return result of operation
  return {keys: keysResult, run: runResult};
}