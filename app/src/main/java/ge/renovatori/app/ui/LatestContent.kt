package ge.renovatori.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ge.renovatori.app.blog.BlogCard
import ge.renovatori.app.blog.fetchBlogs
import ge.renovatori.app.network.ApiClient
import ge.renovatori.app.network.ApiError
import ge.renovatori.app.project.ProjectCard
import ge.renovatori.app.project.fetchProjects
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** The "latest projects / latest posts" blocks shown under detail pages and the calculator. */
data class LatestContent(val projects: List<ProjectCard> = emptyList(), val blogs: List<BlogCard> = emptyList())

/**
 * Loads both lists in parallel. They are secondary content, so a failure leaves that list empty
 * instead of failing the page. [excludeBlogId] drops the post being read, like the web sidebar.
 */
suspend fun ApiClient.fetchLatestContent(projectCount: Int, blogCount: Int, excludeBlogId: String? = null): LatestContent =
    coroutineScope {
        val projects = async {
            try {
                if (projectCount > 0) fetchProjects(page = 1, pageSize = projectCount).items else emptyList()
            } catch (e: ApiError) {
                emptyList()
            }
        }
        val blogs = async {
            try {
                // Ask for one extra so the list stays full after removing the current post.
                val requested = blogCount + if (excludeBlogId != null) 1 else 0
                if (blogCount > 0) fetchBlogs(page = 1, pageSize = requested).items.filter { it.id != excludeBlogId }.take(blogCount) else emptyList()
            } catch (e: ApiError) {
                emptyList()
            }
        }
        LatestContent(projects.await(), blogs.await())
    }

/** Project cards under a heading (web "ბოლო 3 პროექტი" / "ჩვენი ნამუშევრები"); hidden when empty. */
@Composable
fun LatestProjectsSection(title: String, projects: List<ProjectCard>, showDescription: Boolean = false) {
    if (projects.isEmpty()) return
    val navigator = LocalAppNavigator.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title)
        projects.forEach { project ->
            ContentCard(
                title = project.title,
                onClick = { navigator.navigate(Routes.project(project.id)) },
                imageUrl = project.coverImageUrl,
                description = if (showDescription) project.metaDescription.ifBlank { "პროექტის მოკლე აღწერა ხელმისაწვდომი არ არის." } else null,
                footer = if (showDescription) null else formatDate(project.updatedAt ?: project.startDate)
            )
        }
    }
}

/** Blog cards with descriptions under a heading (calculator "უახლესი ბლოგები"); hidden when empty. */
@Composable
fun LatestBlogCardsSection(title: String, blogs: List<BlogCard>) {
    if (blogs.isEmpty()) return
    val navigator = LocalAppNavigator.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title)
        blogs.forEach { blog ->
            ContentCard(
                title = blog.title,
                onClick = { navigator.navigate(Routes.blog(blog.id)) },
                imageUrl = blog.coverImageUrl,
                description = blog.metaDescription.ifBlank { "ბლოგის მოკლე აღწერა ხელმისაწვდომი არ არის." }
            )
        }
    }
}

/** Compact "ბოლო 10 ბლოგი" card (the web detail-page sidebar). */
@Composable
fun LatestBlogsSidebar(blogs: List<BlogCard>) {
    val navigator = LocalAppNavigator.current

    OutlinedCard(Modifier.fillMaxWidth()) {
        Text(
            "ბოლო 10 ბლოგი",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
                .semantics { heading() }
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            // The sidebar keeps its heading with an explanation when there is nothing to list.
            if (blogs.isEmpty()) {
                Text(
                    "ბოლო ბლოგები ვერ მოიძებნა.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            blogs.forEachIndexed { index, blog ->
                if (index > 0) HorizontalDivider()
                CompactContentRow(blog.title, blog.coverImageUrl, blog.updatedAt ?: blog.createdAt) {
                    navigator.navigate(Routes.blog(blog.id))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
}
