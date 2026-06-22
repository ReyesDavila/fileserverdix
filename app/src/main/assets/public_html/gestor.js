var selectedRow = null, lastFileCount = 0, CONFIG = {}, currentFiles = [], sortCol = 2, sortAsc = false, allSelected = false;

var ACTION_MAP = {
    'fetch': { act: 'fetch', cb: function(res) { lastFileCount = res.files.length; render(res); } },
    'upload': { act: 'upload', cb: function(res) { /* Se maneja individualmente en el bucle */ } },
    'check_status': { act: 'check_status', cb: function(res) { /* El manejo se hace en el startPolling */ } },
    'delete': { act: 'delete', cb: function(res) { if (res.status == "ok") init(); } },
    'delete_all': { act: 'delete_all', cb: function(res) { if (res.status == "ok") init(); } },
    'download': { act: 'download', cb: function(res) { startPolling(res); } },
    'download_all': { act: 'download_all', cb: function(res) { startPolling(res); } },
    'view': { act: 'view', cb: function(res) { if (res.status == "ok") window.open(res.url, '_blank'); } }
};

function startPolling(res) {
    if (res.status !== "ok") { mtf("", "clear"); mtf("Error iniciando descarga"); return; }
    var ticket = res.ticket;
    var interval = setInterval(function() {
        execute('check_status', { ticket: ticket }, function(statusRes) {
            if (statusRes.ready) {
                clearInterval(interval);
                mtf("", "clear");
                window.location.href = statusRes.url;
            }
        });
    }, 1000);
}

function loadConfig(cb) { try { var x = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP"); x.open("GET", "api_contract.json", false); x.send(null); CONFIG = eval('(' + x.responseText + ')'); cb(); } catch (e) { mtf("Error contrato"); } }

function mtf(msg, mode) { 
    if (mode === 'clear') { var els = document.getElementsByClassName('mtf-perm'); for (var i = els.length - 1; i >= 0; i--) document.body.removeChild(els[i]); return; } 
    var d = document.createElement("div"); d.style.position = "fixed"; d.style.top = "20px"; d.style.right = "20px"; d.style.padding = "10px"; d.style.zIndex = "9999"; d.style.color = "#fff"; d.style.background = "#333"; d.className = (mode === 'perm' ? 'mtf-perm' : ''); d.innerText = msg; document.body.appendChild(d); 
    if (!mode) setTimeout(function() { document.body.removeChild(d); }, 3000); 
}

function execute(actionKey, params, customCb) { 
    try { 
        var cmd = ACTION_MAP[actionKey]; 
        var x = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP"); 
        x.open("POST", CONFIG.base_path, true); 
        x.setRequestHeader("Content-Type", "application/json"); 
        x.onreadystatechange = function() { 
            if (x.readyState == 4 && x.status == 200) { 
                var res = eval('(' + x.responseText + ')'); 
                if (customCb) customCb(res); else cmd.cb(res); 
            } 
        }; 
        params.action = cmd.act; 
        x.send(JSON.stringify(params)); 
    } catch (e) { mtf("Error: " + actionKey); } 
}

function clearTable() { var t = document.getElementById("tabla-archivos"); for (var i = t.rows.length - 1; i > 0; i--) t.deleteRow(i); }

function render(data) { 
    if (data.ip) document.getElementById("info-servidor").innerText = "Servidor: " + data.ip + ":" + data.port; 
    if (data.files) currentFiles = data.files; 
    currentFiles.sort(function(a, b) { var vA = sortCol == 1 ? a.name.toLowerCase() : a.date, vB = sortCol == 1 ? b.name.toLowerCase() : b.date; return sortAsc ? (vA > vB ? 1 : -1) : (vA < vB ? 1 : -1); }); 
    clearTable(); 
    var t = document.getElementById("tabla-archivos"); 
    for (var i = 0; i < currentFiles.length; i++) { 
        (function(f, idx) { 
            var r = t.insertRow(-1); 
            var isSelected = (allSelected || selectedRow == f.name); 
            r.style.backgroundColor = isSelected ? "#555555" : "#333333"; 
            r.onclick = function() { selectRow(r, f.name); }; 
            r.ondblclick = function() { execute('view', { filename: f.name }); }; 
            r.insertCell(0).className = "col-num"; r.cells[0].innerText = idx + 1; 
            r.insertCell(1).innerText = f.name; r.insertCell(2).innerText = f.date; 
        })(currentFiles[i], i); 
    } 
}

function selectRow(tr, name) { allSelected = false; var r = document.getElementById("tabla-archivos").rows; for (var i = 1; i < r.length; i++) { var n = r[i].cells[1].innerText; r[i].style.backgroundColor = (n == name) ? "#555555" : "#333333"; } selectedRow = name; }
function sortTable(col) { sortAsc = (sortCol == col) ? !sortAsc : false; sortCol = col; render({ files: currentFiles }); }

document.getElementById("id-num").onclick = function() { allSelected = !allSelected; selectedRow = allSelected ? "TODOS" : null; var r = document.getElementById("tabla-archivos").rows; for (var i = 1; i < r.length; i++) r[i].style.backgroundColor = allSelected ? "#555555" : "#333333"; };
document.getElementById("id-nombre").onclick = function() { sortTable(1); };
document.getElementById("id-fecha").onclick = function() { sortTable(2); };

// CAMBIO AQUÍ: Soporte para múltiples archivos
document.getElementById("input-subir").onchange = function() {
    var files = this.files;
    if (!files || files.length === 0) return;
    
    mtf("Subiendo " + files.length + " archivos...", "perm");
    
    var processedCount = 0;
    for (var i = 0; i < files.length; i++) {
        (function(file) {
            var r = new FileReader();
            r.onload = function(e) {
                execute('upload', { filename: file.name, data: e.target.result.split(',')[1] }, function(res) {
                    processedCount++;
                    if (processedCount === files.length) {
                        mtf("", "clear");
                        init(); // Recargar la tabla una vez terminados todos
                    }
                });
            };
            r.readAsDataURL(file);
        })(files[i]);
    }
};

document.getElementById("btn-borrar").onclick = function() { if (!selectedRow) return; if (confirm("¿Borrar " + selectedRow + "?")) { execute(selectedRow == "TODOS" ? 'delete_all' : 'delete', selectedRow == "TODOS" ? {} : { filename: selectedRow }); } };
document.getElementById("btn-bajar").onclick = function() { if (!selectedRow) return; mtf("Solicitando descarga...", "perm"); if (selectedRow == "TODOS") execute('download_all', {}); else execute('download', { filename: selectedRow }); };
document.getElementById("info-servidor").onclick = function() { var url = this.innerText.replace("Servidor: ", ""); var t = document.createElement("textarea"); t.value = url; document.body.appendChild(t); t.select(); document.execCommand('copy'); document.body.removeChild(t); mtf("URL copiada"); };

function init() { execute('fetch', {}); }
setInterval(function() { execute('fetch', {}); }, 3000);
window.onload = function() { loadConfig(init); };