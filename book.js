module.exports = {
    root: "./docs",

    title: "Testcontainers",

    structure: {
        readme: "index.md"
    },

    plugins: [
        "-highlight",
        "prism",
        "github",
        "expandable-chapters",
    ],
    "pluginsConfig": {
        "github": {
            "url": "https://github.com/testcontainers/testcontainers-java/"
        }
    },
};
