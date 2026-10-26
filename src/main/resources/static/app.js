// plain JS, no framework. Only calls the REST API and shows results,
// all calculation and tenant checks are on the server

const state = { tenant: null, batchId: null, categories: [], conditions: [] };
const $ = (id) => document.getElementById(id);

async function api(method, path, body) {
    const response = await fetch(path, {
        method,
        headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': state.tenant },
        body: body ? JSON.stringify(body) : undefined,
    });
    if (response.status === 204) return null;
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || `Klaida ${response.status}`);
    return data;
}

function showError(message) {
    $('error').textContent = message;
    $('error').hidden = !message;
}

const eur = (value) => value == null ? '' : Number(value).toFixed(2).replace('.', ',') + ' €';
const percent = (value) => Math.round(Number(value) * 100) + ' %';
const today = () => new Date().toISOString().slice(0, 10);
const daysAgo = (days) => new Date(Date.now() - days * 86400000).toISOString().slice(0, 10);
const flags = (list) => list.map((f) => `<span class="flag">${f}</span>`).join(' ');

// --- tenants ---

async function loadTenants() {
    const tenants = await fetch('/api/tenants').then((r) => r.json());
    $('tenant').innerHTML = tenants.map((t) => `<option value="${t.id}">${t.name} (${t.id})</option>`).join('');
    $('tenant').onchange = () => selectTenant(tenants.find((t) => t.id === $('tenant').value));
    await selectTenant(tenants[0]);
}

async function selectTenant(tenant) {
    state.tenant = tenant.id;
    $('tenant-settings').textContent = `antkainis ${percent(tenant.targetMarkup)}, minimalus ${percent(tenant.minMarkup)}, `
        + `kanalo mokestis ${percent(tenant.channelFee)}`;
    state.categories = await api('GET', '/api/categories');
    state.batchId = null;
    $('details').hidden = true;
    $('create').hidden = true;
    showError('');
    await loadBatches();
}

// --- batch list + details ---

async function loadBatches() {
    const batches = await api('GET', '/api/batches');
    $('batch-list').innerHTML = batches.map((b) =>
        `<li data-id="${b.id}" class="${b.id === state.batchId ? 'active' : ''}">
            <strong>#${b.id} ${b.name}</strong><br>
            <span class="muted">${b.itemCount} prek., S = ${eur(b.totalCost)}</span></li>`).join('')
        || '<li class="muted">Partijų nėra</li>';
    document.querySelectorAll('#batch-list li[data-id]').forEach((li) =>
        li.onclick = () => openBatch(Number(li.dataset.id)));
}

async function openBatch(id) {
    showError('');
    try {
        const batch = await api('GET', `/api/batches/${id}`);
        state.batchId = batch.id;
        $('create').hidden = true;
        $('details').hidden = false;
        $('batch-title').textContent = `#${batch.id} ${batch.name}`;
        $('batch-summary').textContent = `Pirkimas ${eur(batch.purchasePrice)} + pristatymas ${eur(batch.shippingCost)} `
            + `+ kitos ${eur(batch.otherCosts)} = savikaina S ${eur(batch.totalCost)}`;
        $('items').innerHTML = batch.items.map((item, i) => `<tr>
            <td>${i + 1}</td><td>${item.category}</td><td>${item.condition}</td>
            <td class="num">${eur(item.manualCost)}</td><td class="num">${eur(item.marketPrice)}</td>
            <td class="num"><strong>${eur(item.cost)}</strong></td>
            <td class="num"><strong>${eur(item.recommendedPrice)}</strong></td>
            <td class="num">${eur(item.minPrice)}</td><td>${item.listedOn || ''}</td>
            <td>${flags(item.flags)}</td></tr>`).join('');
        await loadRepricing();
        await loadBatches();
    } catch (e) {
        $('details').hidden = true;
        showError(e.message);
    }
}

async function loadRepricing() {
    const date = $('repricing-date').value || today();
    const rows = await api('GET', `/api/batches/${state.batchId}/repricing?date=${date}`);
    $('repricing').innerHTML = rows.map((r, i) => `<tr>
        <td>${i + 1}</td><td>${r.category}</td><td>${r.listedOn}</td>
        <td class="num">${eur(r.listedPrice)}</td><td class="num"><strong>${eur(r.newPrice)}</strong></td>
        <td class="num">${eur(r.minPrice)}</td>
        <td>${r.steps.map((s) => Number(s.price)).join(' → ')}</td>
        <td>${r.needsReview ? flags(['reikia peržiūrėti']) : ''}</td></tr>`).join('')
        || '<tr><td colspan="8" class="muted">Šią dieną perkainoti nereikia</td></tr>';
}

async function deleteBatch() {
    if (!confirm('Ištrinti partiją?')) return;
    try {
        await api('DELETE', `/api/batches/${state.batchId}`);
        state.batchId = null;
        $('details').hidden = true;
        await loadBatches();
    } catch (e) {
        showError(e.message);
    }
}

// --- new batch form ---

function addItemRow(item = {}) {
    const categoryOptions = state.categories.map((c) =>
        `<option ${c.name === item.category ? 'selected' : ''}>${c.name}</option>`).join('');
    const conditionOptions = state.conditions.map((c) =>
        `<option value="${c.code}" ${c.code === (item.condition || 'LABAI_GERA') ? 'selected' : ''}>${c.label}</option>`)
        .join('');
    const row = document.createElement('tr');
    row.innerHTML = `
        <td><select name="category">${categoryOptions}</select></td>
        <td><select name="condition">${conditionOptions}</select></td>
        <td><input name="quantity" type="number" min="1" value="${item.quantity || 1}"></td>
        <td><input name="manualCost" type="number" step="0.01" min="0" value="${item.manualCost ?? ''}"></td>
        <td><input name="marketPrice" type="number" step="0.01" min="0" value="${item.marketPrice ?? ''}"></td>
        <td><input name="listedOn" type="date" value="${item.listedOn || ''}"></td>
        <td><button type="button" title="Pašalinti">✕</button></td>`;
    row.querySelector('button').onclick = () => row.remove();
    $('item-rows').appendChild(row);
}

function fillForm(batch) {
    const form = $('batch-form');
    for (const key of ['name', 'purchasePrice', 'shippingCost', 'otherCosts']) {
        form.elements[key].value = batch[key] ?? '';
    }
    $('item-rows').innerHTML = '';
    batch.items.forEach(addItemRow);
}

const exampleBatch = () => ({
    name: 'Outlet dėžė #2', purchasePrice: 180, shippingCost: 20, otherCosts: 0,
    items: [
        { category: 'striukė', manualCost: 50, listedOn: today() },
        { category: 'džinsai', quantity: 3, listedOn: daysAgo(28) },
        { category: 'marškinėliai', quantity: 9, listedOn: today() },
    ],
});

const errorBatch = () => ({
    name: 'Bloga partija', purchasePrice: 200, shippingCost: 0, otherCosts: 0,
    items: [
        { category: 'striukė', manualCost: 120 },
        { category: 'striukė', manualCost: 90 },
    ],
});

function readForm() {
    const form = $('batch-form');
    const number = (value) => value === '' ? null : Number(value);
    return {
        name: form.elements.name.value,
        purchasePrice: number(form.elements.purchasePrice.value),
        shippingCost: number(form.elements.shippingCost.value),
        otherCosts: number(form.elements.otherCosts.value),
        items: [...$('item-rows').rows].map((row) => {
            const field = (name) => row.querySelector(`[name=${name}]`).value;
            return {
                category: field('category'),
                condition: field('condition'),
                quantity: number(field('quantity')),
                manualCost: number(field('manualCost')),
                marketPrice: number(field('marketPrice')),
                listedOn: field('listedOn') || null,
            };
        }),
    };
}

async function submitBatch(event) {
    event.preventDefault();
    showError('');
    try {
        const batch = await api('POST', '/api/batches', readForm());
        await openBatch(batch.id);
    } catch (e) {
        showError(e.message);
    }
}

function showCreateForm() {
    showError('');
    $('details').hidden = true;
    $('create').hidden = false;
    fillForm({ name: '', items: [{}] });
}

// --- init ---

async function init() {
    state.conditions = await fetch('/api/conditions').then((r) => r.json());
    $('repricing-date').value = today();
    $('repricing-date').onchange = loadRepricing;
    $('new-batch-btn').onclick = showCreateForm;
    $('delete-btn').onclick = deleteBatch;
    $('add-row').onclick = () => addItemRow();
    $('fill-example').onclick = () => fillForm(exampleBatch());
    $('fill-error').onclick = () => fillForm(errorBatch());
    $('batch-form').onsubmit = submitBatch;
    $('open-by-id').onsubmit = (event) => {
        event.preventDefault();
        openBatch(Number($('batch-id').value));
    };
    await loadTenants();
}

init().catch((e) => showError(e.message));
