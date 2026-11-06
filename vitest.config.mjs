import {defineConfig} from 'vitest/config';

// Unit tests of the front end only: never the Java tests or anything under node_modules
export default defineConfig({
    test: {
        include: ['src/test/javascript/**/*.test.js'],
        environment: 'node'
    }
});
