/**
 * Rehype plugin that automatically generates FAQPage JSON-LD structured data
 * from FAQ markdown content. Extracts h3/h4 headings ending with "?" and their
 * following paragraph(s) as answers.
 */

function textContent(node) {
    if (node.type === 'text') {
        return node.value;
    }
    if (node.children) {
        return node.children.map(textContent).join('');
    }
    return '';
}

export default function rehypeFaqSchema() {
    return (tree, file) => {
        if (!file.history[0]?.match(/[/\\]faq\.md$/)) {
            return;
        }

        const entries = [];
        const children = tree.children || [];

        for (let i = 0; i < children.length; i++) {
            const node = children[i];
            if (node.type !== 'element' || !['h3', 'h4'].includes(node.tagName)) {
                continue;
            }

            const question = textContent(node).trim();
            if (!question.endsWith('?')) {
                continue;
            }

            // Collect following paragraph text as the answer
            const parts = [];
            for (let j = i + 1; j < children.length; j++) {
                const next = children[j];
                if (next.type === 'element' && ['h2', 'h3', 'h4'].includes(next.tagName)) {
                    break;
                }
                if (next.type === 'element' && next.tagName === 'p') {
                    parts.push(textContent(next).trim());
                }
            }

            if (parts.length > 0) {
                entries.push({
                    '@type': 'Question',
                    name: question,
                    acceptedAnswer: {
                        '@type': 'Answer',
                        text: parts.join(' '),
                    },
                });
            }
        }

        if (entries.length === 0) {
            return;
        }

        tree.children.push({
            type: 'element',
            tagName: 'script',
            properties: {type: 'application/ld+json'},
            children: [{
                type: 'text',
                value: JSON.stringify({
                    '@context': 'https://schema.org',
                    '@type': 'FAQPage',
                    mainEntity: entries,
                }),
            }],
        });
    };
}
