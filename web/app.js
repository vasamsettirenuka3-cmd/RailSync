/* Update menus and hide technical labels. */
document.querySelectorAll('.technical-details, .tag')
  .forEach(el => el.remove());

const algorithmTable = document.querySelector('#dash table.map');
if (algorithmTable) algorithmTable.closest('.card').remove();

const searchMethod = document.querySelector('#t-search [name="algo"]');
if (searchMethod) searchMethod.remove();

const labels = {
  dash: 'Dashboard',
  m1: 'Search Trains & Alerts',
  m2: 'Search & Compare Documents',
  m3: 'Route Planning & Data Tools',
  m4: 'Passenger Flow & Platforms',
  m5: 'Scheduling & Critical Stations',
  m6: 'Delays & Passenger Statistics'
};

Object.entries(labels).forEach(([id, text]) => {
  const link = document.querySelector('nav a[href="#' + id + '"]');

  if (link) {
    const icon = link.querySelector('i');
    link.replaceChildren();
    if (icon) link.append(icon);
    link.append(document.createTextNode(' ' + text));
  }

  const heading = document.querySelector('#' + id + ' .sec-head h2');
  if (heading) heading.textContent = text;
});

const cardLabels = {
  't-search': 'Search Railway Records',
  't-alerts': 'Check Alert Keywords',
  't-patterns': 'Check Repeating Events',
  't-sim': 'Compare Service Documents',
  't-dsearch': 'Search Service Documents',
  't-correct': 'Find the Correct Station Name',
  't-route': 'Plan a Round Trip',
  't-matrix': 'Order Data Calculations',
  't-obst': 'Improve Station Lookups',
  't-flow': 'Check Passenger Capacity',
  't-platforms': 'Assign Train Platforms',
  't-sat': 'Check Scheduling Rules',
  't-vc': 'Choose Stations to Monitor',
  't-sort': 'View Train Delays',
  't-res': 'Sample Railway Events',
  't-prime': 'Check a Prime Number',
  't-scan': 'View Passenger Totals'
};

Object.entries(cardLabels).forEach(([id, text]) => {
  const heading = document.querySelector('#' + id + ' h3');
  if (heading) heading.textContent = text;
});

const patternHint = document.querySelector('#t-patterns .hint');
if (patternHint) {
  patternHint.textContent =
    'A = arrive, D = depart, L = late. Check whether the event sequence repeats.';
}

const lookupHint = document.querySelector('#t-obst .hint');
if (lookupHint) {
  lookupHint.textContent =
    'Frequently searched stations are placed where they take fewer steps to find.';
}

const foot = document.querySelector('.side-foot');
if (foot) foot.textContent = 'RailSync · Railway Operations Tools';

const footer = document.querySelector('footer');
if (footer) footer.textContent = 'RailSync · Railway Operations Platform';

/* Shared helpers */
const $ = (s, r = document) => r.querySelector(s);

const $$ = (s, r = document) =>
  Array.from(r.querySelectorAll(s));

const esc = s => String(s).replace(/[&<>"]/g, c => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;'
}[c]));

const fmt = n => Number(n).toLocaleString('en-IN');

async function get(url) {
  const r = await fetch(url);
  const j = await r.json();

  if (j.error) throw new Error(j.error);

  return j;
}

const metrics = list =>
  '<div class="metrics">' +
  list.map(m => `
    <div class="metric ${m[2] || ''}">
      <span>${esc(m[0])}</span>
      <b>${esc(m[1])}</b>
    </div>
  `).join('') +
  '</div>';

const table = (head, rows) =>
  '<div class="tbl"><table><thead><tr>' +
  head.map(h => `<th>${esc(h)}</th>`).join('') +
  '</tr></thead><tbody>' +
  rows.map(r =>
    '<tr>' +
    r.map(c => `<td>${c}</td>`).join('') +
    '</tr>'
  ).join('') +
  '</tbody></table></div>';

const chip = (text, cls = '') =>
  `<span class="chip ${cls}">${esc(text)}</span>`;

const note = (html, cls = '') =>
  `<div class="note ${cls}">${html}</div>`;

function mark(text, ranges) {
  const on = new Array(text.length).fill(false);

  ranges.forEach(([start, length]) => {
    for (let i = start; i < start + length && i < text.length; i++) {
      on[i] = true;
    }
  });

  let out = '';
  let open = false;

  for (let i = 0; i < text.length; i++) {
    if (on[i] && !open) {
      out += '<mark>';
      open = true;
    }

    if (!on[i] && open) {
      out += '</mark>';
      open = false;
    }

    out += esc(text[i]);
  }

  return out + (open ? '</mark>' : '');
}

function tool(id, fn) {
  const card = $('#' + id);
  const out = $('.out', card);

  const run = async () => {
    out.innerHTML = '<div class="loading">Running&hellip;</div>';

    try {
      out.innerHTML = await fn(card);
    } catch (e) {
      out.innerHTML = `<div class="error">${esc(e.message)}</div>`;
    }
  };

  $('.run', card).addEventListener('click', run);

  card.addEventListener('keydown', e => {
    if (e.key === 'Enter' && e.target.tagName === 'INPUT') {
      run();
    }
  });

  return run;
}

const val = (card, name) =>
  $(`[name=${name}]`, card).value;

const qs = obj =>
  Object.keys(obj)
    .map(k => k + '=' + encodeURIComponent(obj[k]))
    .join('&');

const stationOptions = (names, selected) =>
  names.map((name, i) => `
    <option value="${i}" ${selected.includes(i) ? 'selected' : ''}>
      ${esc(name)}
    </option>
  `).join('');

/* Search railway records */
tool('t-search', async c => {
  const d = await get('/api/search?' + qs({
    q: val(c, 'q'),
    scope: val(c, 'scope'),
    algo: 'kmp'
  }));

  return metrics([
    ['Matches', d.matches],
    ['Records found', d.records.length]
  ]) + (
    d.records.length
      ? table(
          ['Record type', 'Details'],
          d.records.map(r => [
            esc(r.type),
            mark(r.text, r.pos.map(p => [p, d.patternLen]))
          ])
        )
      : note('No records found. Try another search.')
  );
});

/* Check alert keywords */
tool('t-alerts', async c => {
  const d = await get('/api/alerts?' + qs({
    text: val(c, 'text')
  }));

  const rows = d.alerts.map(a => {
    const ranges = a.hits.map(h => [h.pos, h.keyword.length]);

    const keywords = [...new Set(a.hits.map(h => h.keyword))]
      .map(k => chip(k, 'bad'))
      .join('');

    return [
      mark(a.text.toLowerCase(), ranges),
      keywords || '<span class="hint">No keywords found</span>'
    ];
  });

  const maximum = Math.max(1, ...d.counts);

  const frequency = d.keywords.map((keyword, i) => `
    <div class="tree-row">
      <span style="width:120px">${esc(keyword)}</span>
      <div class="bar hot" style="flex:1">
        <div style="width:${d.counts[i] / maximum * 100}%"></div>
      </div>
      <b>${d.counts[i]}</b>
    </div>
  `).join('');

  return table(['Alert', 'Keywords found'], rows) +
    '<h4>Keyword frequency</h4>' + frequency;
});

/* Check repeating events */
tool('t-patterns', async c => {
  const d = await get('/api/patterns?' + qs({
    s: val(c, 's')
  }));

  return metrics([
    ['Events', d.s.length],
    ['Repeating cycle', d.periodic ? 'Yes' : 'No'],
    ['Cycle length', d.period],
    ['Repetitions', d.repeats]
  ]) + (
    d.periodic
      ? note(`Repeated event sequence: <b>${esc(d.unit)}</b>`, 'ok')
      : note('No repeating cycle found.')
  );
});

/* Compare service documents */
tool('t-sim', async c => {
  const d = await get('/api/docs/similar?' + qs({
    a: val(c, 'a'),
    b: val(c, 'b')
  }));

  return metrics([
    ['Similarity', d.similarity + '%'],
    ['Shared text length', d.lcsLen + ' characters']
  ]) +
    '<h4>Longest shared text</h4>' +
    note(esc(d.lcs) || 'No shared text found.') +
    '<h4>Repeated text in the first document</h4>' +
    note(esc(d.repeated) || 'No repeated text found.');
});

/* Search service documents */
tool('t-dsearch', async c => {
  const d = await get('/api/docs/search?' + qs({
    q: val(c, 'q')
  }));

  return metrics([['Matches', d.total]]) +
    table(
      ['Document', 'Text', 'Matches'],
      d.docs.map(x => [
        esc(x.title),
        mark(x.text, x.pos.map(p => [p, d.len])),
        x.pos.length
      ])
    );
});

/* Suggest correct station names */
tool('t-correct', async c => {
  const d = await get('/api/correct?' + qs({
    word: val(c, 'word')
  }));

  if (!d.suggestions.length) {
    return note('No station suggestions found.');
  }

  return note(
    `Did you mean <b>${esc(d.suggestions[0].station)}</b>?`
  ) + table(
    ['Suggested station'],
    d.suggestions.map(x => [esc(x.station)])
  );
});

/* Plan a round trip */
tool('t-route', async c => {
  const stations = $$(
    'option:checked',
    $('[name=stations]', c)
  ).map(o => o.value).join(',');

  const d = await get('/api/route?' + qs({ stations }));

  return metrics([
    ['Stations', d.count],
    ['Best round trip', d.cost + ' km'],
    ['Your selection order', d.naive + ' km'],
    ['Distance saved', (d.naive - d.cost) + ' km']
  ]) +
    '<h4>Round-trip route</h4>' +
    d.order.map(x => chip(x)).join(' &rarr; ') +
    (d.order.length ? ' &rarr; ' + chip(d.order[0]) : '');
});

/* Order data calculations */
tool('t-matrix', async c => {
  const d = await get('/api/matrix?' + qs({
    dims: val(c, 'dims')
  }));

  return metrics([
    ['Operations', d.matrices],
    ['Recommended calculation cost', fmt(d.cost)],
    ['Original calculation cost', fmt(d.naive)]
  ]) +
    '<h4>Recommended order</h4>' +
    note(esc(d.paren));
});

/* Improve station lookups */
tool('t-obst', async () => {
  const d = await get('/api/obst');

  return metrics([
    ['Average lookup steps', d.avg],
    ['Comparison average', d.avgBalanced]
  ]) + table(
    ['Station', 'Lookup level', 'Access frequency'],
    d.nodes.map(n => [
      esc(n.station),
      n.depth,
      n.freq
    ])
  );
});

/* Check passenger capacity */
tool('t-flow', async c => {
  const d = await get('/api/flow?' + qs({
    source: val(c, 'source'),
    sink: val(c, 'sink')
  }));

  return metrics([
    ['Maximum passengers per hour', fmt(d.flow)],
    ['Bottleneck capacity', fmt(d.cutCap)]
  ]) +
    `<h4>${esc(d.source)} &rarr; ${esc(d.sink)}</h4>` +
    '<h4>Capacity bottlenecks</h4>' +
    d.cut.map(e =>
      chip(`${e.from} → ${e.to} (${e.cap})`, 'bad')
    ).join('') +
    '<h4>Track usage</h4>' +
    table(
      ['Track', 'Capacity', 'Assigned flow'],
      d.edges.filter(e => e.flow > 0).map(e => [
        esc(e.from) + ' &rarr; ' + esc(e.to),
        e.cap,
        e.flow
      ])
    );
});

/* Assign train platforms */
tool('t-platforms', async () => {
  const d = await get('/api/platforms');

  return metrics([
    ['Trains', d.trains],
    ['Platforms', d.platforms],
    ['Trains assigned', d.matching],
    ['All trains assigned', d.perfect ? 'Yes' : 'No']
  ]) + table(
    ['Train', 'Eligible platforms', 'Assigned platform'],
    d.assignments.map(a => [
      esc(a.train),
      esc(a.eligible),
      a.platform
        ? chip(a.platform, 'ok')
        : chip('Unassigned', 'bad')
    ])
  );
});

/* Check scheduling rules */
tool('t-sat', async c => {
  const d = await get('/api/sat?' + qs({
    clauses: val(c, 'clauses'),
    preset: c.dataset.preset || ''
  }));

  c.dataset.preset = '';

  return note(
    d.sat
      ? 'These scheduling rules can be satisfied together.'
      : 'These scheduling rules conflict. Change the rules and try again.',
    d.sat ? 'ok' : 'bad'
  ) + (
    d.sat
      ? table(
          ['Service', 'Runs in peak slot'],
          d.assignment.map((v, i) => [
            'Service ' + (i + 1),
            v ? 'Yes' : 'No'
          ])
        )
      : ''
  );
});

$('#t-sat [data-preset]').addEventListener('click', () => {
  const c = $('#t-sat');

  $('[name=clauses]', c).value = '1,1,1;-1,-1,-1';
  c.dataset.preset = 'unsat';

  $('.run', c).click();
});

/* Choose stations to monitor */
tool('t-vc', async () => {
  const d = await get('/api/vc');

  return metrics([
    ['Stations', d.stations],
    ['Tracks', d.tracks],
    ['Recommended monitoring stations', d.approxSize],
    ['Smallest set for this network', d.exactSize]
  ]) +
    '<h4>Recommended stations to monitor</h4>' +
    d.approx.map(x => chip(x, 'warn')).join('') +
    '<h4>Smallest monitoring set</h4>' +
    d.exact.map(x => chip(x, 'ok')).join('') +
    note('Each track connects to at least one station in each listed set.');
});

/* View train delays */
tool('t-sort', async c => {
  const d = await get('/api/sort?' + qs({
    k: val(c, 'k')
  }));

  return table(
    ['Rank', 'Train', 'Delay'],
    d.top.map((t, i) => [
      i + 1,
      esc(t.no + ' ' + t.name),
      t.delay + ' min'
    ])
  );
});

/* Sample railway events */
tool('t-res', async c => {
  const d = await get('/api/reservoir?' + qs({
    k: val(c, 'k')
  }));

  return metrics([
    ['Events selected', d.k],
    ['Events in stream', fmt(d.streamSize)]
  ]) + table(
    ['Selected event'],
    d.sample.map(x => [esc(x)])
  );
});

/* Check a prime number */
tool('t-prime', async c => {
  const d = await get('/api/prime?' + qs({
    n: val(c, 'n')
  }));

  return note(
    d.prime
      ? 'This number is prime.'
      : 'This number is not prime.',
    d.prime ? 'ok' : ''
  ) + note(
    'A prime number is a whole number greater than 1 divisible only by 1 and itself.'
  );
});

/* View passenger totals */
tool('t-scan', async () => {
  const d = await get('/api/scan');

  return metrics([
    ['Total passengers', fmt(d.sequentialSum)]
  ]) + table(
    [
      'Hour',
      'Passengers',
      'Total through this hour',
      'Total before this hour'
    ],
    d.hourly.map((v, h) => [
      String(h).padStart(2, '0') + ':00',
      fmt(v),
      fmt(d.inclusive[h]),
      fmt(d.exclusive[h])
    ])
  );
});

/* Load dashboard data */
(async function init() {
  const status = $('#status');

  try {
    const m = await get('/api/meta');

    status.className = 'status ok';
    status.innerHTML = '<span class="dot"></span> Server connected';

    const cards = [
      ['Stations', m.stations.length, '#2456e8'],
      ['Trains', m.trains, '#7a3ff2'],
      ['Tickets', m.tickets, '#e8741f'],
      ['Live alerts', m.alerts, '#d6403f'],
      ['Track links', m.tracks, '#12a06a']
    ];

    $('#kpis').innerHTML = cards.map(x => `
      <div class="kpi" style="--k:${x[2]}">
        <span>${x[0]}</span>
        <b>${x[1]}</b>
      </div>
    `).join('');

    const docs = m.docs.map((d, i) => `
      <option value="${i}">Doc ${i}: ${esc(d)}</option>
    `).join('');

    $('#t-sim [name=a]').innerHTML = docs;
    $('#t-sim [name=b]').innerHTML = docs;
    $('#t-sim [name=b]').value = '1';

    $('#t-route [name=stations]').innerHTML =
      stationOptions(m.stations, [0, 1, 2, 3, 5, 9]);

    $('#t-flow [name=source]').innerHTML =
      stationOptions(m.stations, [9]);

    $('#t-flow [name=sink]').innerHTML =
      stationOptions(m.stations, [8]);

  } catch (e) {
    status.className = 'status bad';
    status.innerHTML = '<span class="dot"></span> Server unavailable';
  }
})();

/* Sidebar navigation */
const sidebar = $('#sidebar');

$('#burger').addEventListener('click', () => {
  sidebar.classList.toggle('open');
});

$$('nav a').forEach(a => {
  a.addEventListener('click', () => {
    sidebar.classList.remove('open');
  });
});

const sections = $$('main section');

addEventListener('scroll', () => {
  let current = sections[0].id;

  sections.forEach(section => {
    if (section.getBoundingClientRect().top < 140) {
      current = section.id;
    }
  });

  $$('nav a').forEach(a => {
    a.classList.toggle(
      'active',
      a.getAttribute('href') === '#' + current
    );
  });
}, { passive: true });