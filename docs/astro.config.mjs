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
            social: [
                { icon: 'github', label: 'GitHub', href: 'https://github.com/osiegmar/FastCSV' },
            ],
            sidebar: [
                {
                    label: 'Welcome',
                    link: '/'
                },
                {
                    label: 'Guides',
                    autogenerate: {directory: 'guides', collapsed: true},
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
                    label: 'Other libraries',
                    link: '/other-libraries/'
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
                    tag: 'link',
                    attrs: {
                        rel: 'icon',
                        href: '/favicon.ico',
                        sizes: '48x48',
                    },
                },
                {
                    tag: 'link',
                    attrs: {
                        rel: 'apple-touch-icon' ,
                        sizes: '180x180' ,
                        href: '/apple-touch-icon.png'
                    },
                },
                {
                    tag: 'meta',
                    attrs: {
                        property: 'og:image',
                        content: 'https://fastcsv.org/fastcsv-og.png'
                    },
                },
                {
                    tag: 'script',
                    attrs: {
                        src: 'https://plausible.io/js/pa-Qud0x2AyRvZLwCR2FLK77.js',
                        async: true
                    },
                },
                {
                    tag: 'script',
                    content: 'window.plausible=window.plausible||function(){(plausible.q=plausible.q||[]).push(arguments)},plausible.init=plausible.init||function(i){plausible.o=i||{}};plausible.init();'
                },
            ],
            expressiveCode: {
                shiki: {
                    langs: [
                        JSON.parse(fs.readFileSync('./grammars/ABNF.tmLanguage.json', 'utf-8'))
                    ],
                },
            },
        }),
    ]
});
