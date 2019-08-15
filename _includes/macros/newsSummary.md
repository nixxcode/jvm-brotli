## Other changes introduced from {{ include.from }}.0 to {{ include.to }}.0
{% assign newsposts = (site.posts | where: "category" , "news") | sort: 'date' %}

<ul>
    {% for post in newsposts %}
        {% assign minorVersion = post.version | slice: 0, 3 %}
        {% if minorVersion == include.from %}
            <li><code>{{ post.version }}</code> {{ post.summary }}</li>
        {% endif %}
    {% endfor %}
</ul>
