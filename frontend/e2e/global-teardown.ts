import { execFileSync } from 'node:child_process';
import { resolve } from 'node:path';

const frontend = resolve(__dirname, '..');

export default function globalTeardown(): void {
  execFileSync(
    'docker',
    [
      'compose',
      '-p',
      'certame-e2e',
      '-f',
      '../docker-compose.yml',
      '-f',
      '../docker-compose.e2e.yml',
      'down',
      '-v',
      '--remove-orphans',
    ],
    { cwd: frontend, stdio: 'inherit' },
  );
}
