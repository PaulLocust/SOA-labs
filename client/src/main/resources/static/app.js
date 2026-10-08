'use strict';

/* =====================================================================
 * Справочники
 * ===================================================================== */

const ENUMS = {
    furnish: {DESIGNER: 'Дизайнерская', NONE: 'Без отделки', BAD: 'Плохая', LITTLE: 'Минимальная'},
    view: {YARD: 'Во двор', BAD: 'Плохой', NORMAL: 'Обычный', GOOD: 'Хороший', TERRIBLE: 'Ужасный'},
    transport: {FEW: 'Мало', NONE: 'Нет', ENOUGH: 'Достаточно'},
};

/** Поля сортировки из спецификации (параметр sort). */
const SORT_FIELDS = {
    'id': 'id',
    'name': 'Название',
    'coordinates.x': 'Координата X',
    'coordinates.y': 'Координата Y',
    'creationDate': 'Дата создания',
    'area': 'Площадь',
    'price': 'Цена',
    'balcony': 'Балкон',
    'numberOfRooms': 'Комнат',
    'furnish': 'Отделка',
    'view': 'Вид',
    'transport': 'Транспорт',
    'house.name': 'Дом: название',
    'house.year': 'Дом: год',
    'house.numberOfFloors': 'Дом: этажей',
    'house.numberOfFlatsOnFloor': 'Дом: квартир на этаже',
    'house.numberOfLifts': 'Дом: лифтов',
};

/** Человеко-читаемые названия параметров — для сообщений об ошибках. */
const PARAM_LABELS = {
    id: 'id', id1: 'id1', id2: 'id2', id3: 'id3',
    name: 'Название', coordinatesX: 'Координата X', coordinatesY: 'Координата Y',
    area: 'Площадь', price: 'Цена', balcony: 'Балкон', numberOfRooms: 'Количество комнат',
    furnish: 'Отделка', view: 'Вид из окна', transport: 'Транспорт',
    houseName: 'Название дома', houseYear: 'Год постройки дома', houseNumberOfFloors: 'Этажей в доме',
    houseNumberOfFlatsOnFloor: 'Квартир на этаже', houseNumberOfLifts: 'Лифтов в доме',
    pageNumber: 'Номер страницы', pageSize: 'Размер страницы', sort: 'Сортировка',
    creationDateFrom: 'Создана с', creationDateTo: 'Создана по',
    areaMin: 'Площадь от', areaMax: 'Площадь до', priceMin: 'Цена от', priceMax: 'Цена до',
    numberOfRoomsMin: 'Комнат от', numberOfRoomsMax: 'Комнат до',
    houseYearMin: 'Год дома от', houseYearMax: 'Год дома до',
    cheapest: 'cheapest', 'with-balcony': 'with-balcony',
};

const STATUS_TITLES = {
    0: 'Нет связи с клиентским сервером',
    400: 'Некорректный запрос',
    404: 'Не найдено',
    405: 'Метод не поддерживается',
    406: 'Неподдерживаемый формат ответа',
    422: 'Данные не прошли проверку',
    500: 'Внутренняя ошибка сервиса',
    502: 'Сервис недоступен или вернул ошибку',
    503: 'Сервис недоступен',
    504: 'Сервис не ответил вовремя',
};

const STATUS_HINTS = {
    400: 'Не передан обязательный параметр или значение имеет неверный формат.',
    422: 'Значения нарушают ограничения: поля квартиры, диапазон фильтра или параметры операции.',
    406: 'Сервисы отдают данные только в формате XML.',
};

const FLAT_FIELDS = ['name', 'coordinatesX', 'coordinatesY', 'area', 'price', 'balcony', 'numberOfRooms',
    'furnish', 'view', 'transport'];
const HOUSE_FIELDS = ['houseName', 'houseYear', 'houseNumberOfFloors', 'houseNumberOfFlatsOnFloor',
    'houseNumberOfLifts'];

/* =====================================================================
 * Утилиты DOM
 * ===================================================================== */

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

/** Создаёт элемент: el('td', {class: 'x'}, 'текст', childNode). Текст вставляется как текст (без HTML). */
function el(tag, attrs = {}, ...children) {
    const node = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs || {})) {
        if (value === null || value === undefined || value === false) continue;
        if (key === 'class') node.className = value;
        else if (key.startsWith('on')) node.addEventListener(key.slice(2), value);
        else node.setAttribute(key, value === true ? '' : value);
    }
    for (const child of children.flat()) {
        if (child === null || child === undefined || child === false) continue;
        node.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return node;
}

function fillEnumSelects(root = document) {
    for (const select of $$('select[data-enum]', root)) {
        const values = ENUMS[select.dataset.enum];
        select.replaceChildren(el('option', {value: ''}, select.dataset.empty || '—'));
        for (const [value, label] of Object.entries(values)) {
            select.append(el('option', {value}, `${label} (${value})`));
        }
    }
}

const numberFormat = new Intl.NumberFormat('ru-RU', {maximumFractionDigits: 6});

function fmtNumber(value) {
    if (value === null || value === undefined || value === '') return '—';
    const n = Number(value);
    return Number.isFinite(n) ? numberFormat.format(n) : String(value);
}

function fmtDate(value) {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ru-RU');
}

function fmtEnum(kind, value) {
    if (!value) return '—';
    return ENUMS[kind][value] || value;
}

function plural(n, one, few, many) {
    const mod10 = n % 10, mod100 = n % 100;
    if (mod10 === 1 && mod100 !== 11) return one;
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
    return many;
}

/* =====================================================================
 * Работа с API (XML)
 * ===================================================================== */

class ServiceError extends Error {
    constructor(status, error, message, path) {
        super(message);
        this.status = status;
        this.error = error;
        this.path = path;
    }
}

function buildQuery(params) {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(params || {})) {
        if (value === null || value === undefined || value === '') continue;
        if (Array.isArray(value)) value.forEach(v => search.append(key, v));
        else search.append(key, value);
    }
    return search.toString();
}

/**
 * Выполняет запрос к сервису (через клиентский прокси, пути совпадают с API сервисов).
 * Возвращает разобранный XML-документ; при коде ошибки бросает ServiceError с данными из схемы Error.
 */
async function api(method, path, params) {
    const query = buildQuery(params);
    const url = path + (query ? `?${query}` : '');
    let response;
    try {
        response = await fetch(url, {method, headers: {Accept: 'application/xml'}});
    } catch (e) {
        showLastRequest(method, url, 0, 'нет связи');
        throw new ServiceError(0, 'Network error', `Не удалось выполнить запрос: ${e.message}`, url);
    }
    showLastRequest(method, url, response.status, response.statusText);
    const text = await response.text();
    const doc = text.trim() ? new DOMParser().parseFromString(text, 'application/xml') : null;
    if (!response.ok) {
        const root = doc && doc.documentElement;
        if (root && root.localName === 'error') {
            throw new ServiceError(response.status, childText(root, 'error'), childText(root, 'message'),
                childText(root, 'path'));
        }
        throw new ServiceError(response.status, response.statusText, text.slice(0, 300) || 'Пустой ответ', url);
    }
    if (doc && doc.getElementsByTagName('parsererror').length) {
        throw new ServiceError(response.status, 'Invalid XML', 'Сервис вернул некорректный XML', url);
    }
    return {status: response.status, doc, headers: response.headers};
}

function child(node, name) {
    if (!node) return null;
    for (const c of node.children) if (c.localName === name) return c;
    return null;
}

function childText(node, name) {
    const c = child(node, name);
    return c ? c.textContent : null;
}

function parseHouse(node) {
    if (!node) return null;
    return {
        name: childText(node, 'name'),
        year: childText(node, 'year'),
        numberOfFloors: childText(node, 'numberOfFloors'),
        numberOfFlatsOnFloor: childText(node, 'numberOfFlatsOnFloor'),
        numberOfLifts: childText(node, 'numberOfLifts'),
    };
}

function parseFlat(node) {
    const coordinates = child(node, 'coordinates');
    return {
        id: childText(node, 'id'),
        name: childText(node, 'name'),
        x: childText(coordinates, 'x'),
        y: childText(coordinates, 'y'),
        creationDate: childText(node, 'creationDate'),
        area: childText(node, 'area'),
        price: childText(node, 'price'),
        balcony: childText(node, 'balcony'),
        numberOfRooms: childText(node, 'numberOfRooms'),
        furnish: childText(node, 'furnish'),
        view: childText(node, 'view'),
        transport: childText(node, 'transport'),
        house: parseHouse(child(node, 'house')),
    };
}

function parseFlatPage(doc) {
    const root = doc.documentElement;
    return {
        pageNumber: Number(childText(root, 'pageNumber')),
        pageSize: Number(childText(root, 'pageSize')),
        totalElements: Number(childText(root, 'totalElements')),
        totalPages: Number(childText(root, 'totalPages')),
        items: Array.from(root.children).filter(c => c.localName === 'flat').map(parseFlat),
    };
}

/* =====================================================================
 * Сообщения об ошибках
 * ===================================================================== */

function label(param) {
    return `«${PARAM_LABELS[param] || param}»`;
}

const MESSAGE_TRANSLATIONS = [
    [/^Required parameter '(.+)' is missing$/, (m, p) => `Не указан обязательный параметр ${label(p)}`],
    [/^Parameter '(.+)' must be an integer$/, (m, p) => `${label(p)}: ожидается целое число`],
    [/^Parameter '(.+)' must be a 32-bit integer$/, (m, p) => `${label(p)}: слишком большое число`],
    [/^Parameter '(.+)' must be a number$/, (m, p) => `${label(p)}: ожидается число`],
    [/^Parameter '(.+)' must be a boolean.*$/, (m, p) => `${label(p)}: ожидается true или false`],
    [/^Parameter '(.+)' must be one of: (.+)$/, (m, p, v) => `${label(p)}: допустимые значения — ${v}`],
    [/^Parameter '(.+)' must be a date-time.*$/, (m, p) => `${label(p)}: ожидается дата и время в формате ISO 8601`],
    [/^Parameter '(.+)' must be specified only once$/, (m, p) => `Параметр ${label(p)} указан несколько раз`],
    [/^Parameter '(.+)' must not be negative$/, (m, p) => `${label(p)} не может быть отрицательным`],
    [/^Parameter '(.+)' must be between (\S+) and (\S+)$/, (m, p, a, b) => `${label(p)} должен быть от ${a} до ${b}`],
    [/^Parameter '(.+)' must be greater than (\S+)$/, (m, p, v) => `${label(p)} должен быть больше ${v}`],
    [/^Field '(.+)' must be between (\S+) and (\S+)$/, (m, p, a, b) => `Поле ${label(p)} должно быть от ${a} до ${b}`],
    [/^Field '(.+)' must be greater than (\S+)$/, (m, p, v) => `Поле ${label(p)} должно быть больше ${v}`],
    [/^Field '(.+)' must not be greater than (\S+)$/, (m, p, v) => `Поле ${label(p)} должно быть не больше ${v}`],
    [/^Field '(.+)' must not be empty$/, (m, p) => `Поле ${label(p)} не может быть пустым`],
    [/^(\w+) must not be greater than (\w+)$/, (m, a, b) => `Фильтр ${label(a)} не может быть больше ${label(b)}`],
    [/^Unknown sort field '(.*)'.*$/, (m, f) => `Неизвестное поле сортировки «${f}»`],
    [/^Parameter 'sort' must be in format.*$/, () => 'Неверный формат сортировки (ожидается «поле,asc» или «поле,desc»)'],
    [/^Flat with id (\d+) not found$/, (m, id) => `Квартира с id ${id} не найдена`],
    [/^Flat with id (\d+) has no house.*$/, (m, id) =>
        `У квартиры ${id} нет дома: чтобы добавить его, укажите название, год, число квартир на этаже и лифтов`],
    [/^Parameters houseName.* must be specified together$/, () =>
        'Название дома, год постройки, число квартир на этаже и лифтов указываются вместе'],
    [/^At least one field parameter must be specified$/, () => 'Нужно изменить хотя бы одно поле'],
    [/^No flats with balcony found$/, () => 'Квартир с балконом нет'],
    [/^No flats without balcony found$/, () => 'Квартир без балкона нет'],
    [/^id1, id2 and id3 must be different$/, () => 'id1, id2 и id3 должны быть разными'],
    [/^Only application\/xml is supported$/, () => 'Сервис поддерживает только формат application/xml'],
    [/^Flats service is unavailable$/, () => 'Первый сервис (Flat Collection Service) недоступен'],
    [/^Flats service did not respond in time$/, () => 'Первый сервис не ответил вовремя'],
    [/^Flats service responded with status (\d+)(.*)$/, (m, s, d) => `Первый сервис ответил кодом ${s}${d}`],
    [/^(.+) is unavailable \((.+)\)$/, (m, s, a) => `${s} недоступен по адресу ${a}`],
    [/^Resource (.+) not found$/, (m, r) => `Ресурс ${r} не найден`],
];

function translateMessage(message) {
    if (!message) return '';
    return message.split('; ').map(part => {
        for (const [regex, fn] of MESSAGE_TRANSLATIONS) {
            const match = part.match(regex);
            if (match) return fn(...match);
        }
        return part;
    }).join('\n');
}

function showError(error) {
    if (!(error instanceof ServiceError)) {
        console.error(error);
        toast('error', 'Ошибка', String(error.message || error));
        return;
    }
    const title = error.status
        ? `${error.status} — ${STATUS_TITLES[error.status] || error.error || 'Ошибка'}`
        : STATUS_TITLES[0];
    const translated = translateMessage(error.message);
    const body = el('div', {},
        el('div', {class: 'toast-message'}, translated),
        STATUS_HINTS[error.status] ? el('div', {class: 'toast-hint'}, STATUS_HINTS[error.status]) : null,
        translated !== error.message ? el('div', {class: 'toast-original'}, `Ответ сервиса: ${error.message}`) : null);
    toast('error', title, body, 9000);
}

function toast(kind, title, body, timeout = 4500) {
    const node = el('div', {class: `toast ${kind}`, role: kind === 'error' ? 'alert' : 'status'},
        el('div', {class: 'toast-title'}, title),
        body instanceof Node ? body : el('div', {class: 'toast-message'}, body),
        el('button', {class: 'toast-close', 'aria-label': 'Закрыть', onclick: () => node.remove()}, '✕'));
    $('#toasts').append(node);
    setTimeout(() => node.remove(), timeout);
}

function showLastRequest(method, url, status, statusText) {
    $('#last-request').hidden = false;
    $('#last-request-text').textContent = `${method} ${decodeURIComponent(url)}`;
    const pill = $('#last-request-status');
    const reason = statusText || (status >= 200 && status < 300 ? 'OK' : STATUS_TITLES[status] || '');
    pill.textContent = status ? `${status} ${reason}`.trim() : statusText;
    pill.className = `status-pill ${status >= 200 && status < 300 ? 'ok' : 'fail'}`;
}

/** Блокирует кнопку на время запроса и показывает ошибку, если она возникла. */
async function withBusy(button, action) {
    if (button) button.disabled = true;
    try {
        return await action();
    } catch (e) {
        showError(e);
        return undefined;
    } finally {
        if (button) button.disabled = false;
    }
}

/* =====================================================================
 * Отображение квартиры
 * ===================================================================== */

function houseSummary(house) {
    if (!house) return '—';
    const parts = [`«${house.name}», ${house.year} г.`];
    parts.push(house.numberOfFloors ? `${house.numberOfFloors} эт.` : 'этажность не указана');
    parts.push(`${house.numberOfFlatsOnFloor} кв./этаж`, `${house.numberOfLifts} лифт.`);
    return parts.join(', ');
}

function flatSentence(flat) {
    const rooms = Number(flat.numberOfRooms);
    return `«${flat.name}» (id ${flat.id}) — ${fmtNumber(flat.numberOfRooms)} `
        + `${plural(rooms, 'комната', 'комнаты', 'комнат')}, ${fmtNumber(flat.area)} м², `
        + `цена ${fmtNumber(flat.price)}, ${flat.balcony === 'true' ? 'с балконом' : 'без балкона'}.`;
}

function flatDetails(flat) {
    const rows = [
        ['id', flat.id],
        ['Название', flat.name],
        ['Координаты', `x = ${flat.x}, y = ${flat.y}`],
        ['Дата создания', fmtDate(flat.creationDate)],
        ['Площадь', `${fmtNumber(flat.area)} м²`],
        ['Цена', fmtNumber(flat.price)],
        ['Балкон', flat.balcony === 'true' ? 'есть' : 'нет'],
        ['Комнат', fmtNumber(flat.numberOfRooms)],
        ['Отделка', fmtEnum('furnish', flat.furnish)],
        ['Вид из окна', fmtEnum('view', flat.view)],
        ['Транспорт', fmtEnum('transport', flat.transport)],
    ];
    const list = el('dl', {class: 'details'});
    for (const [term, value] of rows) list.append(el('dt', {}, term), el('dd', {}, value));
    const house = flat.house;
    const houseList = el('dl', {class: 'details'});
    if (house) {
        for (const [term, value] of [
            ['Название', house.name], ['Год постройки', house.year],
            ['Этажей', house.numberOfFloors || 'не указано'], ['Квартир на этаже', house.numberOfFlatsOnFloor],
            ['Лифтов', house.numberOfLifts],
        ]) houseList.append(el('dt', {}, term), el('dd', {}, value));
    }
    return el('div', {class: 'flat-card'},
        el('p', {class: 'flat-sentence'}, flatSentence(flat)),
        el('div', {class: 'details-columns'},
            el('section', {}, el('h3', {}, 'Квартира'), list),
            el('section', {}, el('h3', {}, 'Дом'), house ? houseList : el('p', {class: 'muted'}, 'Дом не указан'))));
}

function showFlatDialog(flat, title = `Квартира №${flat.id}`) {
    $('#view-title').textContent = title;
    $('#view-body').replaceChildren(flatDetails(flat));
    $('#view-actions').replaceChildren(
        el('button', {class: 'btn', onclick: () => { closeDialog('#view-dialog'); openFlatForm('put', flat); }},
            'Изменить'),
        el('button', {class: 'btn', onclick: () => { closeDialog('#view-dialog'); openFlatForm('patch', flat); }},
            'Изменить частично'),
        el('button', {class: 'btn ghost', 'data-close': true, onclick: () => closeDialog('#view-dialog')}, 'Закрыть'));
    $('#view-dialog').showModal();
}

function closeDialog(selector) {
    const dialog = $(selector);
    if (dialog.open) dialog.close();
}

/* =====================================================================
 * Коллекция: фильтры, сортировка, страницы
 * ===================================================================== */

const state = {
    pageNumber: 0,
    pageSize: 10,
    sort: [],          // [{field, dir}]
    filters: {},
    page: null,
};

function readFilters() {
    const filters = {};
    for (const input of $$('#filters-form [data-param]')) {
        const value = input.value.trim();
        if (!value) continue;
        if (input.dataset.type === 'datetime') {
            const date = new Date(value);
            filters[input.dataset.param] = Number.isNaN(date.getTime()) ? value : date.toISOString();
        } else {
            filters[input.dataset.param] = value;
        }
    }
    return filters;
}

function sortParams() {
    return state.sort.map(s => `${s.field},${s.dir}`);
}

async function loadFlats() {
    const params = {
        ...state.filters,
        sort: sortParams(),
        pageNumber: state.pageNumber,
        pageSize: state.pageSize,
    };
    const result = await withBusy($('#btn-reload'), () => api('GET', '/api/flats', params));
    if (!result) return;
    state.page = parseFlatPage(result.doc);
    renderTable();
}

function renderTable() {
    const page = state.page;
    const thead = $('#flats-table thead');
    const tbody = $('#flats-table tbody');
    const columns = [
        ['id', 'id'], ['name', 'Название'], ['coordinates.x', 'X'], ['coordinates.y', 'Y'],
        ['creationDate', 'Создана'], ['area', 'Площадь, м²'], ['price', 'Цена'], ['balcony', 'Балкон'],
        ['numberOfRooms', 'Комнат'], ['furnish', 'Отделка'], ['view', 'Вид'], ['transport', 'Транспорт'],
        ['house.name', 'Дом'], [null, ''],
    ];
    const primary = state.sort[0];
    thead.replaceChildren(el('tr', {}, columns.map(([field, title]) => {
        if (!field) return el('th', {class: 'actions-col'}, 'Действия');
        const active = primary && primary.field === field;
        const arrow = active ? (primary.dir === 'asc' ? ' ↑' : ' ↓') : '';
        return el('th', {
            class: `sortable${active ? ' active' : ''}`,
            title: 'Сортировать по этому полю',
            onclick: () => sortByColumn(field),
        }, title + arrow);
    })));

    tbody.replaceChildren(...page.items.map(flat => el('tr', {},
        el('td', {class: 'num'}, flat.id),
        el('td', {class: 'name-cell'}, el('button', {class: 'link', onclick: () => showFlatDialog(flat)}, flat.name)),
        el('td', {class: 'num'}, fmtNumber(flat.x)),
        el('td', {class: 'num'}, fmtNumber(flat.y)),
        el('td', {class: 'nowrap'}, fmtDate(flat.creationDate)),
        el('td', {class: 'num'}, fmtNumber(flat.area)),
        el('td', {class: 'num'}, fmtNumber(flat.price)),
        el('td', {}, flat.balcony === 'true' ? 'есть' : 'нет'),
        el('td', {class: 'num'}, fmtNumber(flat.numberOfRooms)),
        el('td', {}, fmtEnum('furnish', flat.furnish)),
        el('td', {}, fmtEnum('view', flat.view)),
        el('td', {}, fmtEnum('transport', flat.transport)),
        el('td', {class: 'house-cell'}, houseSummary(flat.house)),
        el('td', {class: 'actions'},
            el('button', {class: 'icon-btn', title: 'Изменить (PUT)', onclick: () => openFlatForm('put', flat)}, '✎'),
            el('button', {class: 'icon-btn', title: 'Изменить частично (PATCH)',
                onclick: () => openFlatForm('patch', flat)}, '◐'),
            el('button', {class: 'icon-btn danger', title: 'Удалить', onclick: () => deleteFlat(flat)}, '🗑')))));

    $('#table-empty').hidden = page.items.length > 0;
    const from = page.items.length ? page.pageNumber * page.pageSize + 1 : 0;
    const to = page.pageNumber * page.pageSize + page.items.length;
    $('#page-summary').textContent = page.totalElements
        ? `Найдено ${fmtNumber(page.totalElements)} ${plural(page.totalElements, 'квартира', 'квартиры', 'квартир')}`
          + (page.items.length ? `, показаны ${from}–${to}` : '')
        : 'Ничего не найдено';
    const totalPages = Math.max(page.totalPages, 1);
    $('#pager-info').textContent = `Страница ${page.pageNumber + 1} из ${totalPages}`;
    $('#btn-first').disabled = $('#btn-prev').disabled = page.pageNumber <= 0;
    $('#btn-next').disabled = $('#btn-last').disabled = page.pageNumber >= page.totalPages - 1;
    $('#goto-page').max = totalPages;

    const filtersCount = Object.keys(state.filters).length;
    $('#filters-count').hidden = !filtersCount;
    $('#filters-count').textContent = filtersCount;
    $('#sort-summary').textContent = state.sort.length
        ? `— ${state.sort.map(s => `${SORT_FIELDS[s.field]} ${s.dir === 'asc' ? '↑' : '↓'}`).join(', ')}`
        : '— по id ↑';
}

function sortByColumn(field) {
    const primary = state.sort[0];
    if (primary && primary.field === field) {
        primary.dir = primary.dir === 'asc' ? 'desc' : 'asc';
    } else {
        state.sort = [{field, dir: 'asc'}, ...state.sort.filter(s => s.field !== field)];
    }
    renderSortRows();
    state.pageNumber = 0;
    loadFlats();
}

function renderSortRows() {
    const container = $('#sort-rows');
    container.replaceChildren(...state.sort.map((s, index) => {
        const fieldSelect = el('select', {'aria-label': 'Поле'},
            Object.entries(SORT_FIELDS).map(([value, title]) =>
                el('option', {value, selected: value === s.field}, title)));
        fieldSelect.addEventListener('change', () => { s.field = fieldSelect.value; });
        const dirSelect = el('select', {'aria-label': 'Направление'},
            el('option', {value: 'asc', selected: s.dir === 'asc'}, 'по возрастанию ↑'),
            el('option', {value: 'desc', selected: s.dir === 'desc'}, 'по убыванию ↓'));
        dirSelect.addEventListener('change', () => { s.dir = dirSelect.value; });
        return el('div', {class: 'sort-row'},
            el('span', {class: 'sort-index'}, `${index + 1}.`), fieldSelect, dirSelect,
            el('button', {class: 'icon-btn', title: 'Убрать', type: 'button', onclick: () => {
                state.sort.splice(index, 1);
                renderSortRows();
            }}, '✕'));
    }));
    if (!state.sort.length) container.append(el('p', {class: 'muted'}, 'Сортировка по умолчанию: по id по возрастанию.'));
}

async function deleteFlat(flat) {
    if (!confirm(`Удалить квартиру «${flat.name}» (id ${flat.id})?`)) return;
    const result = await withBusy(null, () => api('DELETE', `/api/flats/${encodeURIComponent(flat.id)}`));
    if (!result) return;
    toast('success', 'Квартира удалена', `«${flat.name}» (id ${flat.id}) удалена из коллекции.`);
    if (state.page && state.page.items.length === 1 && state.pageNumber > 0) state.pageNumber--;
    loadFlats();
}

/* =====================================================================
 * Форма создания / изменения квартиры
 * ===================================================================== */

const formState = {mode: 'create', flat: null, original: {}};

function flatToFormValues(flat) {
    if (!flat) return {};
    const values = {
        name: flat.name, coordinatesX: flat.x, coordinatesY: flat.y, area: flat.area, price: flat.price,
        balcony: flat.balcony, numberOfRooms: flat.numberOfRooms, furnish: flat.furnish || '',
        view: flat.view || '', transport: flat.transport,
    };
    if (flat.house) {
        Object.assign(values, {
            houseName: flat.house.name, houseYear: flat.house.year,
            houseNumberOfFloors: flat.house.numberOfFloors || '',
            houseNumberOfFlatsOnFloor: flat.house.numberOfFlatsOnFloor,
            houseNumberOfLifts: flat.house.numberOfLifts,
        });
    }
    return values;
}

function openFlatForm(mode, flat = null) {
    const form = $('#flat-form');
    form.reset();
    formState.mode = mode;
    formState.flat = flat;
    formState.original = flatToFormValues(flat);
    for (const name of [...FLAT_FIELDS, ...HOUSE_FIELDS]) {
        form.elements[name].value = formState.original[name] ?? '';
    }
    form.elements.hasHouse.checked = Boolean(flat && flat.house);
    updateHouseFields();

    const titles = {
        create: ['Новая квартира', 'POST /api/flats — id и дата создания будут заданы сервисом.', 'Создать'],
        put: [`Изменение квартиры №${flat && flat.id}`,
            `PUT /api/flats/${flat && flat.id} — все поля квартиры заменяются указанными. `
            + 'Пустые «Отделка», «Вид» и снятый флажок «Дом» означают, что значения не будет.', 'Сохранить'],
        patch: [`Частичное изменение квартиры №${flat && flat.id}`,
            `PATCH /api/flats/${flat && flat.id} — сервису отправляются только изменённые поля.`, 'Сохранить изменения'],
    };
    const [title, hint, submit] = titles[mode];
    $('#flat-form-title').textContent = title;
    $('#flat-form-hint').textContent = hint;
    $('#flat-form-submit').textContent = submit;
    $('#flat-dialog').showModal();
    form.elements.name.focus();
}

function updateHouseFields() {
    const enabled = $('#flat-form').elements.hasHouse.checked;
    for (const input of $$('#house-fields input')) input.disabled = !enabled;
    $('#house-fields').classList.toggle('disabled', !enabled);
}

function collectFlatParams() {
    const form = $('#flat-form');
    const value = name => form.elements[name].value.trim();
    const hasHouse = form.elements.hasHouse.checked;
    const params = {};

    if (formState.mode !== 'patch') {
        for (const name of FLAT_FIELDS) params[name] = value(name);
        if (hasHouse) for (const name of HOUSE_FIELDS) params[name] = value(name);
        return params;
    }

    // PATCH: только изменённые поля
    for (const name of FLAT_FIELDS) {
        if (value(name) !== String(formState.original[name] ?? '')) params[name] = value(name);
    }
    if (hasHouse) {
        const hadHouse = Boolean(formState.flat && formState.flat.house);
        for (const name of HOUSE_FIELDS) {
            if (!hadHouse || value(name) !== String(formState.original[name] ?? '')) params[name] = value(name);
        }
    }
    return params;
}

async function submitFlatForm(event) {
    event.preventDefault();
    const params = collectFlatParams();
    const {mode, flat} = formState;
    const request = {
        create: () => api('POST', '/api/flats', params),
        put: () => api('PUT', `/api/flats/${encodeURIComponent(flat.id)}`, params),
        patch: () => api('PATCH', `/api/flats/${encodeURIComponent(flat.id)}`, params),
    }[mode];
    const result = await withBusy($('#flat-form-submit'), request);
    if (!result) return;
    const saved = parseFlat(result.doc.documentElement);
    closeDialog('#flat-dialog');
    const messages = {
        create: ['Квартира создана', `«${saved.name}» получила id ${saved.id}`
            + (result.headers.get('Location') ? ` (${result.headers.get('Location')})` : '') + '.'],
        put: ['Квартира обновлена', `Все поля квартиры №${saved.id} заменены.`],
        patch: ['Квартира обновлена', `Изменённые поля квартиры №${saved.id} сохранены.`],
    };
    toast('success', ...messages[mode]);
    loadFlats();
}

/* =====================================================================
 * Специальные операции и агентство
 * ===================================================================== */

function showResult(selector, ...content) {
    const box = $(selector);
    box.hidden = false;
    box.replaceChildren(...content);
}

function formValues(form) {
    const values = {};
    for (const element of form.elements) {
        if (!element.name || element.type === 'submit') continue;
        if (element.type === 'radio' && !element.checked) continue;
        values[element.name] = element.value.trim();
    }
    return values;
}

async function averageNumberOfRooms(event) {
    const result = await withBusy(event.currentTarget, () => api('GET', '/api/flats/average-number-of-rooms'));
    if (!result) return;
    const average = Number(childText(result.doc.documentElement, 'average'));
    showResult('#result-average',
        el('p', {class: 'big-number'}, average.toLocaleString('ru-RU', {maximumFractionDigits: 2})),
        el('p', {}, average === 0
            ? 'Коллекция пуста — среднее значение принимается равным 0.'
            : `В среднем в квартире коллекции ${average.toLocaleString('ru-RU', {maximumFractionDigits: 2})} `
              + `${plural(Math.round(average), 'комната', 'комнаты', 'комнат')}.`));
}

async function countByHouse(event) {
    event.preventDefault();
    const params = formValues(event.currentTarget);
    const button = $('button[type=submit]', event.currentTarget);
    const result = await withBusy(button, () => api('GET', '/api/flats/count-by-house-greater-than', params));
    if (!result) return;
    const count = Number(childText(result.doc.documentElement, 'count'));
    showResult('#result-count-house',
        el('p', {class: 'big-number'}, fmtNumber(count)),
        el('p', {}, `${count} ${plural(count, 'квартира находится', 'квартиры находятся', 'квартир находятся')} `
            + `в доме «больше», чем «${params.houseName}» ${params.houseYear} г.`));
}

async function deleteByTransport(event) {
    event.preventDefault();
    const transport = event.currentTarget.elements.transport.value;
    const label = transport ? `«${fmtEnum('transport', transport)}»` : '(значение не выбрано)';
    if (!confirm(`Удалить все квартиры с транспортной доступностью ${label}?`)) return;
    const button = $('button[type=submit]', event.currentTarget);
    const result = await withBusy(button,
        () => api('DELETE', `/api/flats/by-transport/${encodeURIComponent(transport)}`));
    if (!result) return;
    const count = Number(childText(result.doc.documentElement, 'deletedCount'));
    showResult('#result-delete-transport',
        el('p', {class: 'big-number'}, fmtNumber(count)),
        el('p', {}, count
            ? `Удалено ${count} ${plural(count, 'квартира', 'квартиры', 'квартир')} с транспортом ${label}.`
            : `Квартир с транспортом ${label} не было — ничего не удалено.`));
    loadFlats();
}

async function findWithBalcony(event) {
    event.preventDefault();
    const {cheapest, withBalcony} = formValues(event.currentTarget);
    const button = $('button[type=submit]', event.currentTarget);
    const result = await withBusy(button, () => api('GET',
        `/agency/find-with-balcony/${encodeURIComponent(cheapest)}/${encodeURIComponent(withBalcony)}`));
    if (!result) return;
    const flat = parseFlat(result.doc.documentElement);
    const what = `${cheapest === 'true' ? 'Самая дешёвая' : 'Самая дорогая'} квартира `
        + `${withBalcony === 'true' ? 'с балконом' : 'без балкона'}`;
    showResult('#result-find-balcony', el('h3', {}, what), flatDetails(flat));
}

async function mostExpensive(event) {
    event.preventDefault();
    const {id1, id2, id3} = formValues(event.currentTarget);
    const button = $('button[type=submit]', event.currentTarget);
    const path = [id1, id2, id3].map(encodeURIComponent).join('/');
    const result = await withBusy(button, () => api('GET', `/agency/get-most-expensive/${path}`));
    if (!result) return;
    const flat = parseFlat(result.doc.documentElement);
    showResult('#result-most-expensive',
        el('h3', {}, `Самая дорогая из квартир ${id1}, ${id2}, ${id3}`), flatDetails(flat));
}

async function openById(event) {
    event.preventDefault();
    const id = $('#get-id').value.trim();
    const result = await withBusy($('button[type=submit]', event.currentTarget),
        () => api('GET', `/api/flats/${encodeURIComponent(id)}`));
    if (!result) return;
    showFlatDialog(parseFlat(result.doc.documentElement));
}

/* =====================================================================
 * Инициализация
 * ===================================================================== */

function switchTab(name) {
    for (const tab of $$('.tab')) tab.classList.toggle('active', tab.dataset.tab === name);
    for (const panel of $$('.tab-panel')) panel.hidden = panel.id !== `tab-${name}`;
}

function init() {
    fillEnumSelects();
    renderSortRows();

    for (const tab of $$('.tab')) tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    for (const button of $$('[data-close]')) {
        button.addEventListener('click', () => button.closest('dialog').close());
    }

    $('#btn-create').addEventListener('click', () => openFlatForm('create'));
    $('#btn-reload').addEventListener('click', loadFlats);
    $('#form-get-by-id').addEventListener('submit', openById);

    $('#filters-form').addEventListener('submit', event => {
        event.preventDefault();
        state.filters = readFilters();
        state.pageNumber = 0;
        loadFlats();
    });
    $('#btn-reset-filters').addEventListener('click', () => {
        $('#filters-form').reset();
        state.filters = {};
        state.pageNumber = 0;
        loadFlats();
    });

    $('#btn-add-sort').addEventListener('click', () => {
        const used = new Set(state.sort.map(s => s.field));
        const field = Object.keys(SORT_FIELDS).find(f => !used.has(f)) || 'id';
        state.sort.push({field, dir: 'asc'});
        renderSortRows();
    });
    $('#btn-apply-sort').addEventListener('click', () => { state.pageNumber = 0; loadFlats(); });
    $('#btn-reset-sort').addEventListener('click', () => {
        state.sort = [];
        renderSortRows();
        state.pageNumber = 0;
        loadFlats();
    });

    $('#btn-first').addEventListener('click', () => { state.pageNumber = 0; loadFlats(); });
    $('#btn-prev').addEventListener('click', () => { state.pageNumber = Math.max(0, state.pageNumber - 1); loadFlats(); });
    $('#btn-next').addEventListener('click', () => { state.pageNumber++; loadFlats(); });
    $('#btn-last').addEventListener('click', () => {
        state.pageNumber = Math.max(0, (state.page ? state.page.totalPages : 1) - 1);
        loadFlats();
    });
    $('#form-goto').addEventListener('submit', event => {
        event.preventDefault();
        const page = Number($('#goto-page').value);
        // номер страницы передаётся сервису как есть: некорректный номер вернёт ошибку сервиса
        state.pageNumber = Number.isInteger(page) ? page - 1 : 0;
        loadFlats();
    });
    $('#page-size').addEventListener('change', event => {
        state.pageSize = Number(event.target.value);
        state.pageNumber = 0;
        loadFlats();
    });

    $('#flat-form').addEventListener('submit', submitFlatForm);
    $('#flat-form').elements.hasHouse.addEventListener('change', updateHouseFields);

    $('#btn-average').addEventListener('click', averageNumberOfRooms);
    $('#form-count-house').addEventListener('submit', countByHouse);
    $('#form-delete-transport').addEventListener('submit', deleteByTransport);
    $('#form-find-balcony').addEventListener('submit', findWithBalcony);
    $('#form-most-expensive').addEventListener('submit', mostExpensive);

    loadFlats();
}

document.addEventListener('DOMContentLoaded', init);
