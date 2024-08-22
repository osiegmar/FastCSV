import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightLinksValidator from 'starlight-links-validator';

// https://astro.build/config
export default defineConfig({
    site: 'https://fastcsv.org',
    integrations: [
        starlight({
            plugins: [starlightLinksValidator()],
            title: 'FastCSV',
            defaultLocale: 'en',
            description: 'The lightning-fast, dependency-free CSV library for Java that adheres to RFC standards.',
            logo: {
                src: './src/assets/fastcsv.svg',
                alt: 'FastCSV',
                replacesTitle: true,
            },
            editLink: {
                baseUrl: 'https://github.com/osiegmar/FastCSV/edit/main/docs/'
            },
            social: {
                github: 'https://github.com/osiegmar/FastCSV',
            },
            sidebar: [
                {
                    label: 'Welcome',
                    link: '/'
                },
                {
                    label: 'Guides',
                    autogenerate: {directory: 'guides'},
                },
                {
                    label: 'Architecture & Design',
                    autogenerate: {directory: 'architecture'},
                },

                {
                    label: 'FAQ',
                    link: '/faq/'
                },
                {
                    label: 'Further Reading',
                    items: [
                        {
                            label: 'Javadoc',
                            link: 'https://javadoc.io/doc/de.siegmar/fastcsv/',
                            attrs: {target: '_blank', style: 'font-style: italic'},
                        },
                        {
                            label: 'Maven Repository',
                            link: 'https://central.sonatype.com/artifact/de.siegmar/fastcsv',
                            attrs: {target: '_blank', style: 'font-style: italic'},
                        },
                        {
                            label: 'Changelog',
                            link: 'https://github.com/osiegmar/FastCSV/blob/main/CHANGELOG.md',
                            attrs: {target: '_blank', style: 'font-style: italic'},
                        },
                        {
                            label: 'License',
                            link: 'https://github.com/osiegmar/FastCSV/blob/main/LICENSE',
                            attrs: {target: '_blank', style: 'font-style: italic'},
                        }
                    ]
                },
            ],
            head: [
                {
                    tag: 'script',
                    attrs: {
                        src: 'https://plausible.io/js/script.js',
                        'data-domain': 'fastcsv.org',
                        defer: true,
                    },
                },
                {
                    tag: 'link',
                    attrs: {
                        rel: 'icon',
                        href:'/favicon.ico',
                        sizes: '48x48',
                    },
                },
            ],
        }),
    ]
});
