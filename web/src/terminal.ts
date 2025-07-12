type LogLevel = 'log' | 'debug' | 'info' | 'warn' | 'error';

function terminalLog(level: LogLevel, msg: string): void {
  if (msg === '') return;

  const wrapper = document.querySelector('.terminal-content')!;
  const div = document.createElement('div');
  div.className = 'terminal-line lvl-' + level;
  div.innerHTML = msg;
  wrapper.append(div);
  wrapper.scrollTop = wrapper.scrollHeight;
}

export async function setupTerminal(): Promise<void> {
  const prevLog = console.log;
  const prevDebug = console.debug;
  const prevInfo = console.info;
  const prevWarn = console.warn;
  const prevError = console.error;

  const replaceTerminalColors = (text: string): string => {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML
      .replace(/\x1b\[1m/g, '<span style="font-weight: bold">')
      .replace(/\x1b\[2m/g, '<span style="font-weight: bold">')
      .replace(/\x1b\[30m/g, '<span style="color: #000">')
      .replace(/\x1b\[31m/g, '<span style="color: #f00">')
      .replace(/\x1b\[32m/g, '<span style="color: #0f0">')
      .replace(/\x1b\[33m/g, '<span style="color: #ff0">')
      .replace(/\x1b\[34m/g, '<span style="color: #00f">')
      .replace(/\x1b\[35m/g, '<span style="color: #f0f">')
      .replace(/\x1b\[36m/g, '<span style="color: #0ff">')
      .replace(/\x1b\[37m/g, '<span style="color: #fff">')
      .replace(/\x1b\[39m/g, '<span style="color: #fff">')
      .replace(/\x1b\[0m/g, '</span>');
  };

  const argToString = (arg: any): string => {
    if (typeof arg === 'string') {
      return arg;
    } else if (typeof arg === 'object') {
      try {
        return JSON.stringify(arg);
      } catch (e) {
        return String(arg);
      }
    } else {
      return String(arg);
    }
  };

  console.log = function (...args: any[]) {
    prevLog(...args);
    terminalLog('log', replaceTerminalColors(args.map(argToString).join(' ')));
  };

  console.debug = function (...args: any[]) {
    prevDebug(...args);
    if (window.debugMode) {
      terminalLog('debug', replaceTerminalColors(args.map(argToString).join(' ')));
    }
  };

  console.info = function (...args: any[]) {
    prevInfo(...args);
    terminalLog('info', replaceTerminalColors(args.map(argToString).join(' ')));
  };

  console.warn = function (...args: any[]) {
    prevWarn(...args);
    terminalLog('warn', replaceTerminalColors(args.map(argToString).join(' ')));
  };

  console.error = function (...args: any[]) {
    prevError(...args);
    terminalLog('error', replaceTerminalColors(args.map(argToString).join(' ')));
  };

  setupTerminalEvents();
  initGrabber();

  console.debug("Terminal is ready");
}

function setupTerminalEvents(): void {
  const app = document.querySelector('#app')!;
  const terminal = document.querySelector('#terminal')!;

  document.querySelector('#toggle-terminal')!.addEventListener('click', () => {
    toggleTerminal();
  });

  document.querySelector('#toggle-layout')!.addEventListener('click', () => {
    app.classList.toggle('horizontal-layout');
    terminal.classList.remove('terminal-collapsed');
    (terminal as HTMLElement).style.width = '';
    (terminal as HTMLElement).style.minWidth = '';
    (terminal as HTMLElement).style.height = '';
    (terminal as HTMLElement).style.minHeight = '';
  });

  document.querySelector('#clear-terminal')!.addEventListener('click', () => {
    document.querySelector('.terminal-content')!.innerHTML = '';
  });
}

function initGrabber(): void {
  const app = document.querySelector('#app')!;
  const terminal = document.querySelector('#terminal')!;
  const grab = document.querySelector('#terminal .terminal-grab') as HTMLElement;

  let holding = false;
  let initialHeight = 0;
  let initialWidth = 0;
  let startPos = { x: 0, y: 0 };

  grab.addEventListener('mousedown', e => {
    startPos = { x: e.clientX, y: e.clientY };
    initialHeight = parseInt((terminal as HTMLElement).offsetHeight.toString(), 10);
    initialHeight = isNaN(initialHeight) ? 200 : initialHeight;
    initialWidth = parseInt((terminal as HTMLElement).offsetWidth.toString(), 10);
    initialWidth = isNaN(initialWidth) ? 200 : initialWidth;

    holding = true;
    e.stopPropagation();
    e.preventDefault();
  });

  document.addEventListener('mouseup', () => {
    holding = false;
  });

  document.addEventListener('mousemove', e => {
    if (!holding) return;

    const pos = { x: e.clientX, y: e.clientY };
    const diff = { x: -(pos.x - startPos.x), y: -(pos.y - startPos.y) };

    if (app.classList.contains('horizontal-layout')) {
      let newWidth = initialWidth + diff.x;
      if (newWidth < 40) newWidth = 40;
      if (newWidth > window.innerWidth - 40) newWidth = window.innerWidth - 40;

      (terminal as HTMLElement).style.width = `${newWidth}px`;
      (terminal as HTMLElement).style.minWidth = `${newWidth}px`;
      (terminal as HTMLElement).style.height = '100%';
      (terminal as HTMLElement).style.minHeight = '100%';
    } else {
      let newHeight = initialHeight + diff.y;
      if (newHeight < 40) newHeight = 40;
      if (newHeight > window.innerHeight - 40) newHeight = window.innerHeight - 40;

      (terminal as HTMLElement).style.width = '100%';
      (terminal as HTMLElement).style.minWidth = '100%';
      (terminal as HTMLElement).style.height = `${newHeight}px`;
      (terminal as HTMLElement).style.minHeight = `${newHeight}px`;

      if (terminal.classList.contains('terminal-collapsed')) {
        terminal.classList.remove('terminal-collapsed');
      }
    }
  });
}

export function toggleTerminal(): void {
  const app = document.querySelector('#app')!;
  const terminal = document.querySelector('#terminal')!;

  if (!app.classList.contains('horizontal-layout')) {
    terminal.classList.toggle('terminal-collapsed');
  } else {
    app.classList.remove('horizontal-layout');
    terminal.classList.toggle('terminal-collapsed');
  }
}

export function openTerminal(): void {
  const terminal = document.querySelector('#terminal')!;
  terminal.classList.remove('terminal-collapsed');
}
