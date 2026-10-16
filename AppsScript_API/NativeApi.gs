/**
 * Control Gas LP — API separada para la aplicación Android nativa.
 * IMPORTANTE: pega aquí el ID de la hoja de cálculo que ya usa tu aplicación web.
 * No reemplaza ni modifica el proyecto de Apps Script de la app web actual.
 */
const SPREADSHEET_ID = 'PEGA_AQUI_EL_ID_DE_TU_HOJA_DE_CALCULO';
const NATIVE_API_KEY_PROPERTY = 'CONTROL_GAS_LP_NATIVE_API_KEY';
const TZ = 'America/Mexico_City';

function doGet(e) {
  // Endpoint de diagnóstico: no devuelve datos privados ni la clave.
  return jsonResponse_({ok: true, service: 'Control Gas LP API', message: 'Usa POST con tu clave de API.'});
}

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) throw new Error('Solicitud vacía.');
    const request = JSON.parse(e.postData.contents);
    const expected = PropertiesService.getScriptProperties().getProperty(NATIVE_API_KEY_PROPERTY);
    if (!expected || String(request.apiKey || '') !== expected) throw new Error('No autorizado. Revisa la URL y la clave API en la app.');
    const action = String(request.action || '');
    let result;
    switch (action) {
      case 'ping': result = {ok: true, service: 'Control Gas LP API'}; break;
      case 'getAppData': result = getAppData(); break;
      case 'getReport': result = getReport(request.month); break;
      case 'saveClient': result = saveClient(request); break;
      case 'saveSale': result = saveSale(request); break;
      case 'recordPayment': result = recordPayment(request); break;
      case 'saveAgenda': result = saveAgenda(request); break;
      case 'updateAgendaStatus': result = updateAgendaStatus(request.id, request.estado); break;
      case 'saveExpense': result = saveExpense(request); break;
      default: throw new Error('Acción no reconocida: ' + action);
    }
    if (result && typeof result === 'object') result.ok = result.ok !== false;
    return jsonResponse_(result);
  } catch (err) {
    return jsonResponse_({ok: false, error: String(err && err.message ? err.message : err)});
  }
}

/** Ejecuta una vez desde el editor. Conserva la clave si ya existe. */
function setupNativeApi() {
  const ss = db_(); // Verifica que el ID sea correcto y autoriza acceso a la hoja.
  const props = PropertiesService.getScriptProperties();
  let key = props.getProperty(NATIVE_API_KEY_PROPERTY);
  if (!key) {
    key = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '');
    props.setProperty(NATIVE_API_KEY_PROPERTY, key);
  }
  Logger.log('Hoja conectada: ' + ss.getName());
  Logger.log('API_KEY=' + key);
  return 'API lista. Copia API_KEY del Registro de ejecución y guárdala en Control Gas LP Android.';
}

function db_() {
  if (!SPREADSHEET_ID || SPREADSHEET_ID.indexOf('PEGA_AQUI') === 0) {
    throw new Error('Falta pegar el ID real de tu hoja de cálculo en SPREADSHEET_ID.');
  }
  return SpreadsheetApp.openById(SPREADSHEET_ID);
}

function jsonResponse_(value) {
  return ContentService.createTextOutput(JSON.stringify(value))
    .setMimeType(ContentService.MimeType.JSON);
}

function getAppData() {
  const ss = db_();
  const clients = readRows_(ss, 'Clientes');
  const allSales = readRows_(ss, 'Ventas');
  const allPayments = readRows_(ss, 'Pagos');
  const allAgenda = readRows_(ss, 'Agenda');
  const allExpenses = readRows_(ss, 'Gastos');
  const paidBySale = {};
  allPayments.forEach(p => { paidBySale[p['ID Venta']] = (paidBySale[p['ID Venta']] || 0) + num_(p['Importe']); });
  const sales = allSales.map(s => {
    const paid = round2_(paidBySale[s['ID Venta']] || 0);
    const total = num_(s['Importe total']);
    return Object.assign({}, s, { 'Abonado': paid, 'Saldo': round2_(Math.max(0, total - paid)) });
  }).sort((a, b) => String(b['Fecha y hora']).localeCompare(String(a['Fecha y hora'])));
  const pendingSales = sales.filter(s => s['Saldo'] > 0.009);
  const now = new Date();
  const todayKey = Utilities.formatDate(now, TZ, 'yyyy-MM-dd');
  const monthKey = Utilities.formatDate(now, TZ, 'yyyy-MM');
  const monthSales = sales.filter(s => String(s['Fecha y hora']).startsWith(monthKey));
  const todaySales = sales.filter(s => String(s['Fecha y hora']).startsWith(todayKey));
  const monthPayments = allPayments.filter(p => String(p['Fecha y hora']).startsWith(monthKey));
  const monthExpenses = allExpenses.filter(g => String(g['Fecha']).startsWith(monthKey));
  const totalOwed = round2_(pendingSales.reduce((sum, s) => sum + num_(s['Saldo']), 0));
  const expenses = monthExpenses.reduce((sum, g) => sum + num_(g['Importe']), 0);
  const collections = monthPayments.reduce((sum, p) => sum + num_(p['Importe']), 0);
  const activeClients = clients.filter(c => c['Activo'] !== 'No');
  const upcomingAgenda = allAgenda.filter(a => a['Estado'] === 'Pendiente')
    .sort((a, b) => String(a['Fecha y hora']).localeCompare(String(b['Fecha y hora']))).slice(0, 100);
  return {
    clients: activeClients.sort((a, b) => String(a['Nombre']).localeCompare(String(b['Nombre']))),
    sales: sales.slice(0, 100),
    pendingSales: pendingSales,
    payments: allPayments.sort((a, b) => String(b['Fecha y hora']).localeCompare(String(a['Fecha y hora']))).slice(0, 50),
    agenda: allAgenda.sort((a, b) => String(a['Fecha y hora']).localeCompare(String(b['Fecha y hora']))).slice(0, 100),
    expenses: allExpenses.sort((a, b) => String(b['Fecha']).localeCompare(String(a['Fecha']))).slice(0, 50),
    dashboard: {
      ventasHoy: round2_(todaySales.reduce((sum, s) => sum + num_(s['Importe total']), 0)),
      entregasHoy: todaySales.length,
      litrosMes: round2_(monthSales.reduce((sum, s) => sum + num_(s['Litros']), 0)),
      ventasMes: round2_(monthSales.reduce((sum, s) => sum + num_(s['Importe total']), 0)),
      cobrosMes: round2_(collections),
      gastosMes: round2_(expenses),
      flujoMes: round2_(collections - expenses),
      saldoPendiente: totalOwed,
      clientesActivos: activeClients.length,
      agendaPendiente: upcomingAgenda.length,
      mes: monthKey
    }
  };
}

function saveClient(data) {
  const name = clean_(data.nombre);
  if (!name) throw new Error('Escribe el nombre del cliente.');
  const ss = db_();
  const sh = ss.getSheetByName('Clientes');
  const id = newId_('CLI');
  const row = [id, new Date(), name, clean_(data.tipo) || 'Hogar', clean_(data.telefono), clean_(data.direccion), clean_(data.referencia), clean_(data.notas), 'Sí'];
  sh.appendRow(row);
  return { ok: true, id: id };
}

function saveSale(data) {
  const clientId = clean_(data.clienteId);
  const liters = num_(data.litros);
  const price = num_(data.precio);
  const initialPayment = num_(data.pagoInicial);
  if (!clientId) throw new Error('Selecciona un cliente.');
  if (liters <= 0) throw new Error('Los litros deben ser mayores que cero.');
  if (price <= 0) throw new Error('El precio por litro debe ser mayor que cero.');
  const total = round2_(liters * price);
  if (initialPayment < 0 || initialPayment > total) throw new Error('El pago inicial debe estar entre $0 y el importe total.');
  if (initialPayment > 0 && !clean_(data.metodoPago)) throw new Error('Selecciona el método del pago inicial.');
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = db_();
    const client = findById_(ss, 'Clientes', 'ID Cliente', clientId);
    if (!client || client['Activo'] === 'No') throw new Error('El cliente seleccionado no existe o está inactivo.');
    const saleId = newId_('VEN');
    const date = parseDateTime_(data.fechaHora);
    ss.getSheetByName('Ventas').appendRow([saleId, date, clientId, client['Nombre'], round2_(liters), round2_(price), total, clean_(data.notas)]);
    if (initialPayment > 0) {
      ss.getSheetByName('Pagos').appendRow([newId_('PAG'), date, saleId, clientId, client['Nombre'], round2_(initialPayment), clean_(data.metodoPago), 'Pago inicial de la venta']);
    }
    return { ok: true, id: saleId, total: total };
  } finally { lock.releaseLock(); }
}

function recordPayment(data) {
  const saleId = clean_(data.ventaId);
  const amount = round2_(num_(data.importe));
  const method = clean_(data.metodoPago);
  if (!saleId) throw new Error('Selecciona la venta que vas a cobrar.');
  if (amount <= 0) throw new Error('El abono debe ser mayor que cero.');
  if (!method) throw new Error('Selecciona el método de pago.');
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = db_();
    const sale = findById_(ss, 'Ventas', 'ID Venta', saleId);
    if (!sale) throw new Error('No se encontró la venta.');
    const payments = readRows_(ss, 'Pagos').filter(p => p['ID Venta'] === saleId);
    const paid = payments.reduce((sum, p) => sum + num_(p['Importe']), 0);
    const balance = round2_(num_(sale['Importe total']) - paid);
    if (amount > balance + 0.009) throw new Error('El abono supera el saldo pendiente (' + money_(balance) + ').');
    const clientName = sale['Cliente'];
    ss.getSheetByName('Pagos').appendRow([newId_('PAG'), parseDateTime_(data.fechaHora), saleId, sale['ID Cliente'], clientName, amount, method, clean_(data.notas)]);
    return { ok: true, saldoRestante: round2_(balance - amount) };
  } finally { lock.releaseLock(); }
}

function saveAgenda(data) {
  const dateTime = clean_(data.fechaHora);
  if (!dateTime) throw new Error('Indica la fecha y hora.');
  const ss = db_();
  let clientId = clean_(data.clienteId);
  let clientName = '';
  if (clientId) {
    const client = findById_(ss, 'Clientes', 'ID Cliente', clientId);
    if (!client) throw new Error('No se encontró el cliente.');
    clientName = client['Nombre'];
  }
  ss.getSheetByName('Agenda').appendRow([newId_('AGE'), parseDateTime_(dateTime), clientId, clientName, clean_(data.tipo) || 'Suministro', 'Pendiente', clean_(data.notas)]);
  return { ok: true };
}

function updateAgendaStatus(id, status) {
  if (['Pendiente', 'Realizado', 'Cancelado'].indexOf(status) === -1) throw new Error('Estado no válido.');
  const ss = db_();
  const sh = ss.getSheetByName('Agenda');
  const values = sh.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (String(values[i][0]) === String(id)) {
      sh.getRange(i + 1, 6).setValue(status);
      return { ok: true };
    }
  }
  throw new Error('No se encontró el evento de agenda.');
}

function saveExpense(data) {
  const concept = clean_(data.concepto);
  const amount = round2_(num_(data.importe));
  if (!concept) throw new Error('Escribe el concepto del gasto.');
  if (amount <= 0) throw new Error('El importe debe ser mayor que cero.');
  const ss = db_();
  ss.getSheetByName('Gastos').appendRow([newId_('GAS'), parseDate_(data.fecha), clean_(data.categoria) || 'Otros', concept, amount, clean_(data.metodoPago) || 'No especificado', clean_(data.notas)]);
  return { ok: true };
}

function getReport(month) {
  const ss = db_();
  const key = /^\d{4}-\d{2}$/.test(String(month || '')) ? String(month) : Utilities.formatDate(new Date(), TZ, 'yyyy-MM');
  const sales = readRows_(ss, 'Ventas').filter(s => String(s['Fecha y hora']).startsWith(key));
  const allPayments = readRows_(ss, 'Pagos');
  const payments = allPayments.filter(p => String(p['Fecha y hora']).startsWith(key));
  const expenses = readRows_(ss, 'Gastos').filter(g => String(g['Fecha']).startsWith(key));
  const salesById = {};
  readRows_(ss, 'Ventas').forEach(s => { salesById[s['ID Venta']] = s; });
  const paymentsByClient = {};
  const debtByClient = {};
  const paidBySale = {};
  allPayments.forEach(p => {
    paidBySale[p['ID Venta']] = (paidBySale[p['ID Venta']] || 0) + num_(p['Importe']);
  });
  const allSales = readRows_(ss, 'Ventas');
  allSales.forEach(s => {
    const balance = Math.max(0, num_(s['Importe total']) - (paidBySale[s['ID Venta']] || 0));
    if (balance > 0.009) debtByClient[s['Cliente']] = (debtByClient[s['Cliente']] || 0) + balance;
  });
  const totalSales = sales.reduce((sum, s) => sum + num_(s['Importe total']), 0);
  const totalLiters = sales.reduce((sum, s) => sum + num_(s['Litros']), 0);
  const totalCollected = payments.reduce((sum, p) => sum + num_(p['Importe']), 0);
  const totalExpenses = expenses.reduce((sum, g) => sum + num_(g['Importe']), 0);
  const topDebtors = Object.keys(debtByClient).map(name => ({ cliente: name, saldo: round2_(debtByClient[name]) }))
    .sort((a, b) => b.saldo - a.saldo).slice(0, 10);
  const paymentMethods = {};
  payments.forEach(p => { const m = p['Método de pago'] || 'No especificado'; paymentMethods[m] = (paymentMethods[m] || 0) + num_(p['Importe']); });
  return { month: key, totalSales: round2_(totalSales), totalLiters: round2_(totalLiters), totalCollected: round2_(totalCollected), totalExpenses: round2_(totalExpenses), cashFlow: round2_(totalCollected - totalExpenses), transactionCount: sales.length, collectionCount: payments.length, topDebtors: topDebtors, paymentMethods: Object.keys(paymentMethods).map(k => ({ metodo: k, total: round2_(paymentMethods[k]) })).sort((a,b)=>b.total-a.total) };
}

function readRows_(ss, sheetName) {
  const sh = ss.getSheetByName(sheetName);
  if (!sh || sh.getLastRow() < 2) return [];
  const data = sh.getDataRange().getValues();
  const headers = data[0];
  return data.slice(1).filter(row => row.some(v => v !== '' && v !== null)).map(row => {
    const obj = {};
    headers.forEach((h, i) => obj[h] = serializeValue_(row[i], h));
    return obj;
  });
}

function serializeValue_(value, header) {
  if (value instanceof Date) {
    const dateOnly = ['Fecha alta', 'Fecha'].indexOf(header) >= 0;
    return Utilities.formatDate(value, TZ, dateOnly ? 'yyyy-MM-dd' : 'yyyy-MM-dd HH:mm:ss');
  }
  return value;
}

function findById_(ss, sheetName, idHeader, id) {
  return readRows_(ss, sheetName).find(row => String(row[idHeader]) === String(id)) || null;
}
function parseDateTime_(value) {
  const text = clean_(value);
  if (!text) return new Date();
  const normalized = text.replace('T', ' ');
  return Utilities.parseDate(normalized, TZ, normalized.length <= 16 ? 'yyyy-MM-dd HH:mm' : 'yyyy-MM-dd HH:mm:ss');
}
function parseDate_(value) {
  const text = clean_(value);
  return text ? Utilities.parseDate(text, TZ, 'yyyy-MM-dd') : new Date();
}
function newId_(prefix) { return prefix + '-' + Utilities.getUuid().replace(/-/g, '').slice(0, 10).toUpperCase(); }
function clean_(value) { return String(value == null ? '' : value).trim(); }
function num_(value) { const n = Number(value); return Number.isFinite(n) ? n : 0; }
function round2_(value) { return Math.round((num_(value) + Number.EPSILON) * 100) / 100; }
function money_(value) { return '$' + round2_(value).toLocaleString('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
